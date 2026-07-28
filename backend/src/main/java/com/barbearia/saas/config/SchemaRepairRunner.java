package com.barbearia.saas.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repara colunas faltantes no H2 local quando o ddl-auto falhou em migrations antigas.
 * Idempotente: usa ADD COLUMN IF NOT EXISTS.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SchemaRepairRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        reparar(
                "ALTER TABLE produtos ADD COLUMN IF NOT EXISTS preco DECIMAL(12,2) DEFAULT 0 NOT NULL",
                "ALTER TABLE produtos ADD COLUMN IF NOT EXISTS descricao_venda VARCHAR(500)",
                "ALTER TABLE produtos ADD COLUMN IF NOT EXISTS marketplace_ativo BOOLEAN DEFAULT FALSE NOT NULL",
                "ALTER TABLE produtos ADD COLUMN IF NOT EXISTS unidade_id BIGINT",
                "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS aceite_privacidade_em TIMESTAMP",
                "ALTER TABLE clientes ADD COLUMN IF NOT EXISTS aceite_privacidade_em TIMESTAMP",
                "ALTER TABLE barbeiros ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500)",
                "UPDATE produtos SET preco = 0 WHERE preco IS NULL",
                "UPDATE produtos SET marketplace_ativo = FALSE WHERE marketplace_ativo IS NULL"
        );
    }

    private void reparar(String... sqls) {
        for (String sql : sqls) {
            try {
                jdbc.execute(sql);
                log.info("Schema repair OK: {}", sql);
            } catch (Exception e) {
                log.warn("Schema repair ignorado ({}): {}", sql, e.getMessage());
            }
        }
    }
}
