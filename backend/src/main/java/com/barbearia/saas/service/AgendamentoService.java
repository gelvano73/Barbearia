package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Barbeiro;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.enums.StatusAgendamento;
import com.barbearia.saas.domain.repository.AgendamentoRepository;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.BarbeiroRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.agendamento.AgendamentoRequest;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

/** Regras de negócio para criação, atualização e status de agendamentos. */
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final List<StatusAgendamento> STATUS_IGNORADOS_CONFLITO =
            List.copyOf(EnumSet.of(StatusAgendamento.CANCELADO, StatusAgendamento.NAO_COMPARECEU));
    private static final DateTimeFormatter MSG_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final ServicoRepository servicoRepository;
    private final ComissaoService comissaoService;
    private final FidelidadeService fidelidadeService;
    private final NotificacaoService notificacaoService;

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(LocalDate data, Long barbeiroId) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();

        if (data != null) {
            LocalDateTime inicio = data.atStartOfDay();
            LocalDateTime fim = data.plusDays(1).atStartOfDay();
            List<Agendamento> lista = barbeiroId != null
                    ? agendamentoRepository.findByBarbeariaIdAndBarbeiroIdAndDataHoraBetweenOrderByDataHoraAsc(
                    barbeariaId, barbeiroId, inicio, fim)
                    : agendamentoRepository.findByBarbeariaIdAndDataHoraBetweenOrderByDataHoraAsc(
                    barbeariaId, inicio, fim);
            return lista.stream().map(this::toResponse).toList();
        }

        return agendamentoRepository.findByBarbeariaIdOrderByDataHoraDesc(barbeariaId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Busca o registro pelo identificador informado. */
    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** Cria um novo registro. */
    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(request.getClienteId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new NegocioException("Cliente inativo");
        }

        Barbeiro barbeiro = barbeiroRepository.findByIdAndBarbeariaId(request.getBarbeiroId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));
        if (!Boolean.TRUE.equals(barbeiro.getAtivo())) {
            throw new NegocioException("Barbeiro inativo");
        }

        Servico servico = resolverServico(request.getServicoId(), barbeariaId);
        int duracao = resolverDuracao(request, servico);
        String nomeServico = resolverNomeServico(request.getServico(), servico);
        validarConflito(barbeiro.getId(), request.getDataHora(), duracao, null);

        Agendamento agendamento = Agendamento.builder()
                .barbearia(barbearia)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servicoRef(servico)
                .dataHora(request.getDataHora())
                .duracaoMinutos(duracao)
                .servico(nomeServico)
                .observacoes(blankToNull(request.getObservacoes()))
                .status(StatusAgendamento.AGENDADO)
                .build();

        Agendamento salvo = agendamentoRepository.save(agendamento);
        notificacaoService.notificarCliente(
                cliente,
                barbeariaId,
                "Agendamento confirmado",
                "Olá, " + cliente.getNome() + "! Seu horário de " + nomeServico
                        + " com " + barbeiro.getNome() + " foi agendado para "
                        + salvo.getDataHora().format(MSG_FMT) + ".");
        return toResponse(salvo);
    }

    /** Atualiza o registro existente. */
    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = encontrarNaBarbearia(id);
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();

        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(request.getClienteId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Barbeiro barbeiro = barbeiroRepository.findByIdAndBarbeariaId(request.getBarbeiroId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado"));

        Servico servico = resolverServico(request.getServicoId(), barbeariaId);
        int duracao = resolverDuracao(request, servico);
        String nomeServico = resolverNomeServico(request.getServico(), servico);
        validarConflito(barbeiro.getId(), request.getDataHora(), duracao, agendamento.getId());

        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServicoRef(servico);
        agendamento.setDataHora(request.getDataHora());
        agendamento.setDuracaoMinutos(duracao);
        agendamento.setServico(nomeServico);
        agendamento.setObservacoes(blankToNull(request.getObservacoes()));
        if (request.getStatus() != null) {
            agendamento.setStatus(request.getStatus());
        }

        return toResponse(agendamentoRepository.save(agendamento));
    }

    /** Atualiza status. */
    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = encontrarNaBarbearia(id);
        agendamento.setStatus(status);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        if (status == StatusAgendamento.CONCLUIDO) {
            comissaoService.gerarSeNecessario(salvo);
            fidelidadeService.creditarPorAgendamento(salvo);
        }
        return toResponse(salvo);
    }

    /** Cancela o registro ou agendamento. */
    @Transactional
    public void cancelar(Long id) {
        Agendamento agendamento = encontrarNaBarbearia(id);
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new NegocioException("Não é possível cancelar um agendamento já concluído");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
        notificacaoService.notificarCliente(
                agendamento.getCliente(),
                agendamento.getBarbearia().getId(),
                "Agendamento cancelado",
                "Olá, " + agendamento.getCliente().getNome()
                        + "! Seu agendamento de " + (agendamento.getServico() != null
                        ? agendamento.getServico() : "serviço")
                        + " em " + agendamento.getDataHora().format(MSG_FMT)
                        + " foi cancelado.");
    }

    /** Valida conflito. */
    public void validarConflito(Long barbeiroId, LocalDateTime inicio, int duracaoMinutos, Long agendamentoId) {
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
        LocalDateTime janelaInicio = inicio.minusHours(4);
        LocalDateTime janelaFim = fim.plusHours(4);

        List<Agendamento> candidatos = agendamentoRepository.findCandidatosConflito(
                barbeiroId, janelaInicio, janelaFim, STATUS_IGNORADOS_CONFLITO, agendamentoId);

        boolean conflito = candidatos.stream().anyMatch(existente -> {
            LocalDateTime existenteFim = existente.getDataHora().plusMinutes(existente.getDuracaoMinutos());
            return inicio.isBefore(existenteFim) && fim.isAfter(existente.getDataHora());
        });

        if (conflito) {
            throw new NegocioException("Horário indisponível para este barbeiro");
        }
    }

    private Agendamento encontrarNaBarbearia(Long id) {
        return agendamentoRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
    }

    /** Converte a entidade em DTO de resposta. */
    public AgendamentoResponse toResponse(Agendamento agendamento) {
        return toResponse(agendamento, null);
    }

    /** Converte a entidade em DTO de resposta. */
    public AgendamentoResponse toResponse(Agendamento agendamento, Boolean podeAvaliar) {
        return AgendamentoResponse.builder()
                .id(agendamento.getId())
                .clienteId(agendamento.getCliente().getId())
                .clienteNome(agendamento.getCliente().getNome())
                .barbeiroId(agendamento.getBarbeiro().getId())
                .barbeiroNome(agendamento.getBarbeiro().getNome())
                .servicoId(agendamento.getServicoRef() != null ? agendamento.getServicoRef().getId() : null)
                .dataHora(agendamento.getDataHora())
                .duracaoMinutos(agendamento.getDuracaoMinutos())
                .status(agendamento.getStatus())
                .servico(agendamento.getServico() != null && !agendamento.getServico().isBlank()
                        ? agendamento.getServico()
                        : (agendamento.getServicoRef() != null ? agendamento.getServicoRef().getNome() : null))
                .observacoes(agendamento.getObservacoes())
                .podeAvaliar(podeAvaliar)
                .criadoEm(agendamento.getCriadoEm())
                .build();
    }

    private Servico resolverServico(Long servicoId, Long barbeariaId) {
        if (servicoId == null) {
            return null;
        }
        Servico servico = servicoRepository.findByIdAndBarbeariaId(servicoId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new NegocioException("Serviço inativo");
        }
        return servico;
    }

    private int resolverDuracao(AgendamentoRequest request, Servico servico) {
        if (servico != null && servico.getDuracaoMinutos() != null) {
            return servico.getDuracaoMinutos();
        }
        return request.getDuracaoMinutos() != null ? request.getDuracaoMinutos() : 30;
    }

    private String resolverNomeServico(String servicoTexto, Servico servico) {
        if (servico != null) {
            return servico.getNome();
        }
        return blankToNull(servicoTexto);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
