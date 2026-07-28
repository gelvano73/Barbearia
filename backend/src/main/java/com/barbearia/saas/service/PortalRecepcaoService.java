package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.*;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.agendamento.AgendamentoRequest;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.cliente.ClienteRequest;
import com.barbearia.saas.dto.cliente.ClienteResponse;
import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
import com.barbearia.saas.dto.recepcao.*;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/** Regras do portal da recepção (fila de atendimento do dia). */
@Service
@RequiredArgsConstructor
public class PortalRecepcaoService {

    private static final List<StatusFila> FILA_ATIVA =
            List.copyOf(EnumSet.of(StatusFila.AGUARDANDO, StatusFila.EM_ATENDIMENTO));

    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;
    private final BarbeiroService barbeiroService;
    private final FilaAtendimentoRepository filaRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoService pagamentoService;
    private final CaixaService caixaService;

    // ---- Clientes / Agendamentos (manual) ----

    /** Lista clientes. */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes() {
        return clienteService.listar(true);
    }

    /** Cria cliente. */
    @Transactional
    public ClienteResponse criarCliente(ClienteRequest request) {
        return clienteService.criar(request);
    }

    /** Atualiza cliente. */
    @Transactional
    public ClienteResponse atualizarCliente(Long id, ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    /** Lista barbeiros. */
    @Transactional(readOnly = true)
    public List<BarbeiroResponse> listarBarbeiros() {
        return barbeiroService.listar(true);
    }

    /** Lista servicos. */
    @Transactional(readOnly = true)
    public List<ServicoResponse> listarServicos() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        return servicoRepository.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId).stream()
                .map(s -> ServicoResponse.builder()
                        .id(s.getId())
                        .nome(s.getNome())
                        .descricao(s.getDescricao())
                        .preco(s.getPreco())
                        .duracaoMinutos(s.getDuracaoMinutos())
                        .comissaoPercentual(s.getComissaoPercentual())
                        .ativo(s.getAtivo())
                        .build())
                .toList();
    }

    /** Lista agendamentos. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarAgendamentos(LocalDate data) {
        return agendamentoService.listar(data, null);
    }

    /** Cria agendamento. */
    @Transactional
    public AgendamentoResponse criarAgendamento(AgendamentoRequest request) {
        return agendamentoService.criar(request);
    }

    /** Atualiza status agendamento. */
    @Transactional
    public AgendamentoResponse atualizarStatusAgendamento(Long id, StatusAgendamento status) {
        return agendamentoService.atualizarStatus(id, status);
    }

    // ---- Fila ----

    /** Lista fila. */
    @Transactional(readOnly = true)
    public List<FilaResponse> listarFila() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        return filaRepository.findByBarbeariaIdAndStatusInOrderByPrioridadeDescPosicaoAsc(barbeariaId, FILA_ATIVA)
                .stream()
                .map(this::toFila)
                .toList();
    }

    /** Inclui o cliente na fila de atendimento. */
    @Transactional
    public FilaResponse adicionarFila(FilaRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(request.getClienteId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Barbeiro barbeiro = null;
        if (request.getBarbeiroId() != null) {
            barbeiro = barbeiroRepository.findByIdAndBarbeariaId(request.getBarbeiroId(), barbeariaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));
        }
        Servico servico = null;
        if (request.getServicoId() != null) {
            servico = servicoRepository.findByIdAndBarbeariaId(request.getServicoId(), barbeariaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
        }

        int proxima = filaRepository.maxPosicaoAtiva(barbeariaId, FILA_ATIVA) + 1;
        FilaAtendimento item = FilaAtendimento.builder()
                .barbearia(barbearia)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .posicao(proxima)
                .prioridade(Boolean.TRUE.equals(request.getPrioridade()))
                .observacoes(blankToNull(request.getObservacoes()))
                .status(StatusFila.AGUARDANDO)
                .build();

        return toFila(filaRepository.save(item));
    }

    /** Atualiza o status de um item da fila. */
    @Transactional
    public FilaResponse atualizarStatusFila(Long id, StatusFila status) {
        FilaAtendimento item = filaRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item da fila não encontrado"));
        item.setStatus(status);
        return toFila(filaRepository.save(item));
    }

    // ---- Caixa ----

    /** Retorna o caixa aberto (ou atual) da unidade. */
    @Transactional(readOnly = true)
    public CaixaResponse caixaAtual() {
        return caixaService.caixaAtual();
    }

    /** Abre caixa. */
    @Transactional
    public CaixaResponse abrirCaixa(AbrirCaixaRequest request) {
        return caixaService.abrir(request);
    }

    /** Fecha caixa. */
    @Transactional
    public CaixaResponse fecharCaixa(FecharCaixaRequest request) {
        return caixaService.fechar(request);
    }

    /** Registra uma sangria (retirada) no caixa. */
    @Transactional
    public CaixaResponse sangria(MovimentoCaixaRequest request) {
        return caixaService.sangria(request);
    }

    /** Registra um suprimento (entrada) no caixa. */
    @Transactional
    public CaixaResponse suprimento(MovimentoCaixaRequest request) {
        return caixaService.suprimento(request);
    }

    // ---- Pagamentos ----

    /** Lista pagamentos. */
    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarPagamentos(LocalDate data) {
        return pagamentoService.listar(data);
    }

    /** Registra pagamento. */
    @Transactional
    public PagamentoResponse registrarPagamento(PagamentoRequest request) {
        caixaService.getCaixaAberto(); // recepção só recebe com caixa aberto
        return pagamentoService.criar(request);
    }

    private FilaResponse toFila(FilaAtendimento f) {
        return FilaResponse.builder()
                .id(f.getId())
                .clienteId(f.getCliente().getId())
                .clienteNome(f.getCliente().getNome())
                .barbeiroId(f.getBarbeiro() != null ? f.getBarbeiro().getId() : null)
                .barbeiroNome(f.getBarbeiro() != null ? f.getBarbeiro().getNome() : null)
                .servicoId(f.getServico() != null ? f.getServico().getId() : null)
                .servicoNome(f.getServico() != null ? f.getServico().getNome() : null)
                .posicao(f.getPosicao())
                .status(f.getStatus())
                .prioridade(f.getPrioridade())
                .observacoes(f.getObservacoes())
                .criadoEm(f.getCriadoEm())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
