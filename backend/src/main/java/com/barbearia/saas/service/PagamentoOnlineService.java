package com.barbearia.saas.service;

import com.barbearia.saas.domain.enums.PlanoRecurso;

import com.barbearia.saas.domain.entity.Agendamento;
import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.entity.Cliente;
import com.barbearia.saas.domain.entity.Pagamento;
import com.barbearia.saas.domain.entity.Servico;
import com.barbearia.saas.domain.enums.GatewayPagamento;
import com.barbearia.saas.domain.enums.StatusPagamento;
import com.barbearia.saas.domain.repository.AgendamentoRepository;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.domain.repository.ClienteRepository;
import com.barbearia.saas.domain.repository.PagamentoRepository;
import com.barbearia.saas.domain.repository.ServicoRepository;
import com.barbearia.saas.dto.pagamento.PagamentoRequest;
import com.barbearia.saas.dto.pagamento.PagamentoResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Criação de checkouts de pagamento online via gateway (Mercado Pago). */
@Service
@RequiredArgsConstructor
public class PagamentoOnlineService {

    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final PagamentoService pagamentoService;
    private final PlanoAcessoService planoAcessoService;

    /** Cria um pagamento pendente e inicia o checkout no gateway configurado. */
    @Transactional
    public PagamentoResponse criarCheckout(PagamentoRequest request) {
        planoAcessoService.exigirRecurso(PlanoRecurso.PAGAMENTO_ONLINE);
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

        Pagamento pagamento = pagamentoRepository.save(Pagamento.builder()
                .barbearia(barbearia)
                .cliente(cliente)
                .servico(servico)
                .agendamento(agendamento)
                .valor(request.getValor())
                .formaPagamento(request.getFormaPagamento())
                .status(StatusPagamento.PENDENTE)
                .gateway(GatewayPagamento.MERCADOPAGO.name())
                .dataPagamento(request.getDataPagamento())
                .descricao(blankToNull(request.getDescricao()) != null
                        ? request.getDescricao().trim()
                        : servico.getNome() + " · online")
                .build());

        Map<String, Object> preferencia = mercadoPagoClient.createPreference(
                servico.getNome(), request.getValor(), String.valueOf(pagamento.getId()), cliente.getEmail());

        pagamento.setGatewayPaymentId(String.valueOf(preferencia.get("id")));
        pagamento.setCheckoutUrl(String.valueOf(preferencia.get("init_point")));
        pagamento.setGatewayStatus("CRIADO");
        pagamento = pagamentoRepository.save(pagamento);

        return pagamentoService.toResponse(pagamento);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
