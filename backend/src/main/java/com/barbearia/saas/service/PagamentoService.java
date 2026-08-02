package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.StatusCaixa;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.enums.TipoMovimentoCaixa;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
import com.barbearia.saas.event.PagamentoConfirmadoEvent;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Registro e consulta de pagamentos vinculados a serviços/agendamentos. */
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Lista os registros solicitados. */
    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(LocalDate data) {
        LocalDate dia = data != null ? data : LocalDate.now();
        return pagamentoRepository
                .findByBarbeariaIdAndDataPagamentoOrderByCriadoEmDesc(SecurityUtils.getBarbeariaIdAtual(), dia)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Busca o registro pelo identificador informado. */
    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorId(Long id) {
        return toResponse(encontrarNaBarbearia(id));
    }

    /** Busca a entidade do pagamento (uso interno, ex.: geração de recibo). */
    @Transactional(readOnly = true)
    public Pagamento buscarEntidadePorId(Long id) {
        return encontrarNaBarbearia(id);
    }

    /** Cria um novo registro. */
    @Transactional
    public PagamentoResponse criar(PagamentoRequest request) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(request.getClienteId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new NegocioException("Cliente inativo");
        }

        Servico servico = servicoRepository.findByIdAndBarbeariaId(request.getServicoId(), barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new NegocioException("Serviço inativo");
        }

        Agendamento agendamento = null;
        if (request.getAgendamentoId() != null) {
            agendamento = agendamentoRepository.findByIdAndBarbeariaId(request.getAgendamentoId(), barbeariaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
        }

        Caixa caixa = caixaRepository
                .findFirstByBarbeariaIdAndStatusOrderByAbertoEmDesc(barbeariaId, StatusCaixa.ABERTO)
                .orElse(null);

        Pagamento pagamento = pagamentoRepository.save(Pagamento.builder()
                .barbearia(barbearia)
                .caixa(caixa)
                .cliente(cliente)
                .servico(servico)
                .agendamento(agendamento)
                .valor(request.getValor())
                .formaPagamento(request.getFormaPagamento())
                .status(StatusPagamento.PAGO)
                .dataPagamento(request.getDataPagamento())
                .descricao(blankToNull(request.getDescricao()) != null
                        ? request.getDescricao().trim()
                        : servico.getNome() + " · " + request.getFormaPagamento())
                .build());

        if (caixa != null) {
            Usuario usuario = usuarioRepository.findById(SecurityUtils.getUsuarioAtual().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
            movimentoRepository.save(MovimentoCaixa.builder()
                    .caixa(caixa)
                    .usuario(usuario)
                    .tipo(TipoMovimentoCaixa.ENTRADA)
                    .formaPagamento(request.getFormaPagamento())
                    .valor(request.getValor())
                    .descricao(pagamento.getDescricao())
                    .pagamento(pagamento)
                    .build());
        }

        eventPublisher.publishEvent(new PagamentoConfirmadoEvent(this, pagamento.getId()));
        return toResponse(pagamento);
    }

    /** Cancela o registro ou agendamento. */
    @Transactional
    public void cancelar(Long id) {
        Pagamento pagamento = encontrarNaBarbearia(id);
        if (pagamento.getStatus() == StatusPagamento.CANCELADO) {
            throw new NegocioException("Pagamento já cancelado");
        }
        pagamento.setStatus(StatusPagamento.CANCELADO);
        pagamentoRepository.save(pagamento);
    }

    /** Converte a entidade em DTO de resposta. */
    public PagamentoResponse toResponse(Pagamento p) {
        return PagamentoResponse.builder()
                .id(p.getId())
                .caixaId(p.getCaixa() != null ? p.getCaixa().getId() : null)
                .clienteId(p.getCliente() != null ? p.getCliente().getId() : null)
                .clienteNome(p.getCliente() != null ? p.getCliente().getNome() : null)
                .servicoId(p.getServico() != null ? p.getServico().getId() : null)
                .servicoNome(p.getServico() != null ? p.getServico().getNome() : null)
                .agendamentoId(p.getAgendamento() != null ? p.getAgendamento().getId() : null)
                .valor(p.getValor())
                .formaPagamento(p.getFormaPagamento())
                .status(p.getStatus())
                .dataPagamento(p.getDataPagamento())
                .descricao(p.getDescricao())
                .gateway(p.getGateway())
                .gatewayPaymentId(p.getGatewayPaymentId())
                .gatewayStatus(p.getGatewayStatus())
                .checkoutUrl(p.getCheckoutUrl())
                .pixQrCode(p.getPixQrCode())
                .pixCopiaCola(p.getPixCopiaCola())
                .criadoEm(p.getCriadoEm())
                .build();
    }

    private Pagamento encontrarNaBarbearia(Long id) {
        return pagamentoRepository.findByIdAndBarbeariaId(id, SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
