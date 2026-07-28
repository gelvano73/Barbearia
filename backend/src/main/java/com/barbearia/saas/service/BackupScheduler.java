package com.barbearia.saas.service;

import com.barbearia.saas.config.BackupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agendador que dispara backups periódicos conforme configuração. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupProperties properties;

    /** Dispara o backup automático conforme agendamento. */
    @Scheduled(cron = "${app.backup.cron:0 0 3 * * *}")
    public void backupAutomatico() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Iniciando backup automático agendado");
        try {
            backupService.executar();
        } catch (Exception e) {
            log.error("Backup automático falhou: {}", e.getMessage(), e);
        }
    }
}
