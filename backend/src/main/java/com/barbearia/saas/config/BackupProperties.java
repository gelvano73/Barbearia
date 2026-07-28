package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriedades tipadas para configuração de backup automático do banco. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {
    private boolean enabled = true;
    /** Cron padrão: todo dia às 03:00 */
    private String cron = "0 0 3 * * *";
    private String dir = "backups";
    private int retentionDays = 7;
}
