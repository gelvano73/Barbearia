package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Barbearia;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Bloqueia assinaturas vencidas (trial ou plano pago sem renovação). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssinaturaExpiracaoScheduler {

    private final BarbeariaRepository barbeariaRepository;

    /** Varre diariamente assinaturas ativas com vencimento passado. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void bloquearVencidas() {
        List<Barbearia> vencidas = barbeariaRepository
                .findByAssinaturaStatusAndAssinaturaVenceEmBefore(StatusAssinatura.ATIVA, LocalDateTime.now());
        for (Barbearia barbearia : vencidas) {
            barbearia.setAssinaturaStatus(StatusAssinatura.BLOQUEADA);
            barbeariaRepository.save(barbearia);
            log.info("Assinatura bloqueada por vencimento: barbeariaId={}", barbearia.getId());
        }
    }
}
