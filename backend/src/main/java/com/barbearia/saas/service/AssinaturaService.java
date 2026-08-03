package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.dto.assinatura.AssinaturaCheckoutResponse;
import com.barbearia.saas.dto.assinatura.AssinaturaResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/** Consulta e controle da assinatura SaaS (plano, status e vencimento) da barbearia. */
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final BarbeariaRepository barbeariaRepository;
    private final MercadoPagoClient mercadoPagoClient;

    /** === Consultas === */

    /** Retorna a situação atual da assinatura da barbearia autenticada. */
    @Transactional(readOnly = true)
    public AssinaturaResponse getStatus() {
        Barbearia barbearia = barbeariaRepository.findById(SecurityUtils.getBarbeariaIdAtual())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        return toResponse(barbearia);
    }

    /** === Upgrade e pagamento === */

    /** Inicia checkout de upgrade de plano (Mercado Pago ou simulado). */
    @Transactional
    public AssinaturaCheckoutResponse iniciarUpgrade(PlanoAssinatura plano) {
        if (plano == null || plano == PlanoAssinatura.TRIAL) {
            throw new NegocioException("Selecione um plano pago (BASIC, PRO ou ENTERPRISE)");
        }

        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        BigDecimal valor = precoMensal(plano);
        String referencia = "assinatura-" + barbeariaId + "-" + plano.name();
        Map<String, Object> preferencia = mercadoPagoClient.createPreference(
                "Assinatura " + plano.name() + " · " + barbearia.getNome(),
                valor,
                referencia,
                null);

        String preferenciaId = preferencia.get("id") != null ? String.valueOf(preferencia.get("id")) : null;
        String checkoutUrl = preferencia.get("init_point") != null
                ? String.valueOf(preferencia.get("init_point")) : null;
        boolean simulado = preferenciaId != null && preferenciaId.startsWith("simulado-");

        if (preferenciaId != null) {
            barbearia.setMpSubscriptionId(preferenciaId);
            barbeariaRepository.save(barbearia);
        }

        return AssinaturaCheckoutResponse.builder()
                .plano(plano)
                .valor(valor)
                .checkoutUrl(checkoutUrl)
                .preferenciaId(preferenciaId)
                .simulado(simulado)
                .build();
    }

    /**
     * Confirma pagamento de assinatura a partir da referência externa do gateway
     * (formato: assinatura-{barbeariaId}-{PLANO}).
     */
    @Transactional
    public void confirmarPagamentoAssinatura(String externalReference) {
        if (externalReference == null || !externalReference.startsWith("assinatura-")) {
            return;
        }
        String[] parts = externalReference.split("-", 3);
        if (parts.length < 3) {
            return;
        }
        Long barbeariaId;
        try {
            barbeariaId = Long.valueOf(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        PlanoAssinatura plano;
        try {
            plano = PlanoAssinatura.valueOf(parts[2]);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (plano == PlanoAssinatura.TRIAL) {
            return;
        }

        barbeariaRepository.findById(barbeariaId).ifPresent(barbearia -> {
            barbearia.setPlano(plano);
            barbearia.setAssinaturaStatus(StatusAssinatura.ATIVA);
            barbearia.setAssinaturaVenceEm(LocalDateTime.now().plusDays(30));
            barbeariaRepository.save(barbearia);
        });
    }

    /** === Conversões === */

    /** Converte a entidade em DTO de resposta, calculando dias restantes até o vencimento. */
    public AssinaturaResponse toResponse(Barbearia barbearia) {
        LocalDateTime venceEm = barbearia.getAssinaturaVenceEm();
        long diasRestantes = venceEm != null
                ? Math.max(0, Duration.between(LocalDateTime.now(), venceEm).toDays())
                : 0;

        return AssinaturaResponse.builder()
                .barbeariaId(barbearia.getId())
                .plano(barbearia.getPlano())
                .status(barbearia.getAssinaturaStatus())
                .venceEm(venceEm)
                .emTeste(barbearia.getPlano() == PlanoAssinatura.TRIAL)
                .diasRestantes(diasRestantes)
                .build();
    }

    /** === Bloqueio e reativação === */

    /** Bloqueia a assinatura da barbearia (ex.: inadimplência confirmada pelo gateway). */
    @Transactional
    public void bloquear(Long barbeariaId) {
        barbeariaRepository.findById(barbeariaId).ifPresent(barbearia -> {
            barbearia.setAssinaturaStatus(StatusAssinatura.BLOQUEADA);
            barbeariaRepository.save(barbearia);
        });
    }

    /** Reativa a assinatura da barbearia (ex.: pagamento confirmado pelo gateway). */
    @Transactional
    public void reativar(Long barbeariaId) {
        barbeariaRepository.findById(barbeariaId).ifPresent(barbearia -> {
            barbearia.setAssinaturaStatus(StatusAssinatura.ATIVA);
            barbeariaRepository.save(barbearia);
        });
    }

    /** === Auxiliares === */

    private BigDecimal precoMensal(PlanoAssinatura plano) {
        return switch (plano) {
            case BASIC -> new BigDecimal("97.90");
            case PRO -> new BigDecimal("197.90");
            case ENTERPRISE -> new BigDecimal("397.90");
            default -> throw new NegocioException("Plano inválido");
        };
    }
}
