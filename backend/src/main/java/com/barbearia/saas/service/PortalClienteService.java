package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.portal.*;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Regras do portal do cliente (perfil, agendar, reagendar, avaliar). */
@Service
@RequiredArgsConstructor
public class PortalClienteService {

    private static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FOTO_BYTES = 2 * 1024 * 1024;

    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoService agendamentoService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** === Perfil === */

    /** Retorna o perfil do usuário autenticado. */
    @Transactional(readOnly = true)
    public PerfilResponse getPerfil() {
        Cliente cliente = getClienteAtual();
        return toPerfil(cliente);
    }

    /** Atualiza o perfil do usuário autenticado. */
    @Transactional
    public PerfilResponse atualizarPerfil(PerfilUpdateRequest request) {
        Cliente cliente = getClienteAtual();
        if (request.getNome() != null && !request.getNome().isBlank()) {
            cliente.setNome(request.getNome().trim());
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setNome(request.getNome().trim());
            }
        }
        if (request.getTelefone() != null && !request.getTelefone().isBlank()) {
            cliente.setTelefone(request.getTelefone().trim());
        }
        return toPerfil(clienteRepository.save(cliente));
    }

    /** Faz upload e associa a foto ao registro. */
    @Transactional
    public PerfilResponse uploadFoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NegocioException("Arquivo de foto obrigatório");
        }
        if (file.getSize() > MAX_FOTO_BYTES) {
            throw new NegocioException("Foto deve ter no máximo 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !TIPOS_FOTO.contains(contentType)) {
            throw new NegocioException("Formato inválido. Use JPEG, PNG ou WEBP");
        }

        Cliente cliente = getClienteAtual();
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String ext = contentType.equals("image/png") ? ".png"
                    : contentType.equals("image/webp") ? ".webp" : ".jpg";
            String filename = "cliente-" + cliente.getId() + "-" + UUID.randomUUID() + ext;
            Path destino = dir.resolve(filename);
            file.transferTo(destino.toFile());
            cliente.setFotoUrl("/uploads/" + filename);
            return toPerfil(clienteRepository.save(cliente));
        } catch (IOException e) {
            throw new NegocioException("Falha ao salvar foto");
        }
    }

    /** === Agendamentos === */

    /** Lista meus agendamentos. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarMeusAgendamentos() {
        Long clienteId = SecurityUtils.getClienteIdAtual();
        return agendamentoRepository.findByClienteIdOrderByDataHoraDesc(clienteId).stream()
                .map(a -> agendamentoService.toResponse(a, podeAvaliar(a)))
                .toList();
    }

    /** Lista o histórico de agendamentos do cliente. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> historico() {
        Long clienteId = SecurityUtils.getClienteIdAtual();
        return agendamentoRepository.findByClienteIdAndStatusOrderByDataHoraDesc(clienteId, StatusAgendamento.CONCLUIDO)
                .stream()
                .map(a -> agendamentoService.toResponse(a, podeAvaliar(a)))
                .toList();
    }

    /** === Catálogo === */

    /** Lista barbeiros. */
    @Transactional(readOnly = true)
    public List<BarbeiroResponse> listarBarbeiros() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        return barbeiroRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .map(b -> BarbeiroResponse.builder()
                        .id(b.getId())
                        .nome(b.getNome())
                        .telefone(b.getTelefone())
                        .especialidade(b.getEspecialidade())
                        .ativo(b.getAtivo())
                        .criadoEm(b.getCriadoEm())
                        .build())
                .toList();
    }

    /** Lista servicos. */
    @Transactional
    public List<ServicoResponse> listarServicos() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        List<Servico> servicos = servicoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
        if (servicos.isEmpty()) {
            seedServicosPadrao(barbeariaId);
            servicos = servicoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
        }
        return servicos.stream().map(this::toServicoResponse).toList();
    }

    /** === Operações de agenda === */

    /** Cria um agendamento. */
    @Transactional
    public AgendamentoResponse agendar(PortalAgendamentoRequest request) {
        Cliente cliente = getClienteAtual();
        Long barbeariaId = cliente.getBarbearia().getId();

        Barbeiro barbeiro = barbeiroRepository.findByIdAndBarbeariaId(request.getBarbeiroId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));
        if (!Boolean.TRUE.equals(barbeiro.getAtivo())) {
            throw new NegocioException("Barbeiro inativo");
        }

        Servico servico = null;
        int duracao = 30;
        String nomeServico = null;
        if (request.getServicoId() != null) {
            servico = servicoRepository.findByIdAndBarbeariaId(request.getServicoId(), barbeariaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
            duracao = servico.getDuracaoMinutos();
            nomeServico = servico.getNome();
        }

        agendamentoService.validarConflito(barbeiro.getId(), request.getDataHora(), duracao, null);

        Agendamento agendamento = Agendamento.builder()
                .barbearia(cliente.getBarbearia())
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servicoRef(servico)
                .dataHora(request.getDataHora())
                .duracaoMinutos(duracao)
                .servico(nomeServico)
                .observacoes(blankToNull(request.getObservacoes()))
                .status(StatusAgendamento.AGENDADO)
                .build();

        return agendamentoService.toResponse(agendamentoRepository.save(agendamento), false);
    }

    /** Reagenda um horário existente. */
    @Transactional
    public AgendamentoResponse reagendar(Long id, ReagendarRequest request) {
        Agendamento agendamento = getAgendamentoDoCliente(id);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new NegocioException("Não é possível reagendar este agendamento");
        }

        agendamentoService.validarConflito(
                agendamento.getBarbeiro().getId(),
                request.getDataHora(),
                agendamento.getDuracaoMinutos(),
                agendamento.getId());

        agendamento.setDataHora(request.getDataHora());
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        return agendamentoService.toResponse(agendamentoRepository.save(agendamento), false);
    }

    /** Cancela o registro ou agendamento. */
    @Transactional
    public void cancelar(Long id) {
        Agendamento agendamento = getAgendamentoDoCliente(id);
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new NegocioException("Não é possível cancelar um agendamento já concluído");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new NegocioException("Agendamento já cancelado");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    /** === Avaliações === */

    /** Registra uma avaliação do atendimento. */
    @Transactional
    public AvaliacaoResponse avaliar(AvaliacaoRequest request) {
        Cliente cliente = getClienteAtual();
        Agendamento agendamento = getAgendamentoDoCliente(request.getAgendamentoId());

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new NegocioException("Só é possível avaliar atendimentos concluídos");
        }
        if (avaliacaoRepository.existsByAgendamentoId(agendamento.getId())) {
            throw new NegocioException("Este atendimento já foi avaliado");
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .barbearia(cliente.getBarbearia())
                .agendamento(agendamento)
                .cliente(cliente)
                .barbeiro(agendamento.getBarbeiro())
                .nota(request.getNota())
                .comentario(blankToNull(request.getComentario()))
                .build();

        Avaliacao salva = avaliacaoRepository.save(avaliacao);
        return AvaliacaoResponse.builder()
                .id(salva.getId())
                .agendamentoId(agendamento.getId())
                .barbeiroId(agendamento.getBarbeiro().getId())
                .barbeiroNome(agendamento.getBarbeiro().getNome())
                .nota(salva.getNota())
                .comentario(salva.getComentario())
                .criadoEm(salva.getCriadoEm())
                .build();
    }

    /** === Auxiliares === */

    private void seedServicosPadrao(Long barbeariaId) {
        Barbearia barbearia = clienteRepository.findById(SecurityUtils.getClienteIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"))
                .getBarbearia();
        if (!barbearia.getId().equals(barbeariaId)) {
            throw new NegocioException("Barbearia inválida");
        }
        List.of(
                servico(barbearia, "Corte Masculino", "Corte tradicional", "45.00", 30, "40"),
                servico(barbearia, "Barba", "Aparar e modelar barba", "35.00", 20, "40"),
                servico(barbearia, "Sobrancelha", "Design de sobrancelha", "20.00", 15, "30"),
                servico(barbearia, "Pigmentação", "Pigmentação capilar", "80.00", 45, "35"),
                servico(barbearia, "Hidratação", "Hidratação capilar", "50.00", 30, "30"),
                servico(barbearia, "Corte Infantil", "Corte para crianças", "35.00", 25, "40"),
                servico(barbearia, "Combo Corte + Barba", "Pacote completo", "70.00", 50, "40")
        ).forEach(servicoRepository::save);
    }

    private Servico servico(Barbearia barbearia, String nome, String desc, String preco, int duracao, String comissao) {
        return Servico.builder()
                .barbearia(barbearia)
                .nome(nome)
                .descricao(desc)
                .preco(new BigDecimal(preco))
                .duracaoMinutos(duracao)
                .comissaoPercentual(new BigDecimal(comissao))
                .ativo(true)
                .build();
    }

    private boolean podeAvaliar(Agendamento agendamento) {
        return agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                && !avaliacaoRepository.existsByAgendamentoId(agendamento.getId());
    }

    private Cliente getClienteAtual() {
        return clienteRepository.findById(SecurityUtils.getClienteIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    private Agendamento getAgendamentoDoCliente(Long id) {
        return agendamentoRepository.findByIdAndClienteId(id, SecurityUtils.getClienteIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
    }

    private PerfilResponse toPerfil(Cliente cliente) {
        return PerfilResponse.builder()
                .clienteId(cliente.getId())
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .fotoUrl(cliente.getFotoUrl())
                .barbeariaId(cliente.getBarbearia().getId())
                .nomeBarbearia(cliente.getBarbearia().getNome())
                .build();
    }

    private ServicoResponse toServicoResponse(Servico servico) {
        return ServicoResponse.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .preco(servico.getPreco())
                .duracaoMinutos(servico.getDuracaoMinutos())
                .comissaoPercentual(servico.getComissaoPercentual())
                .ativo(servico.getAtivo())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
