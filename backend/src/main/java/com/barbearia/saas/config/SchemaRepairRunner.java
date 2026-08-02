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
                "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cpf VARCHAR(11)",
                "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefone VARCHAR(20)",
                "ALTER TABLE clientes ADD COLUMN IF NOT EXISTS aceite_privacidade_em TIMESTAMP",
                "ALTER TABLE barbeiros ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS plano VARCHAR(30) DEFAULT 'TRIAL'",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS assinatura_status VARCHAR(30) DEFAULT 'ATIVA'",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS assinatura_vence_em TIMESTAMP",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS mp_customer_id VARCHAR(80)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS mp_subscription_id VARCHAR(80)",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway VARCHAR(30)",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(120)",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway_status VARCHAR(60)",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(500)",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS pix_qr_code CLOB",
                "ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS pix_copia_cola CLOB",
                "ALTER TABLE clientes ADD COLUMN IF NOT EXISTS cpf VARCHAR(11)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS razao_social VARCHAR(150)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS inscricao_municipal VARCHAR(30)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS codigo_municipio_ibge VARCHAR(7)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS aliquota_iss DECIMAL(5,2) DEFAULT 0",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS codigo_servico_padrao VARCHAR(10) DEFAULT '6.02'",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS regime_tributario VARCHAR(30) DEFAULT 'SIMPLES_NACIONAL'",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS optante_simples BOOLEAN DEFAULT TRUE",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS nfse_habilitada BOOLEAN DEFAULT FALSE",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS nfse_token VARCHAR(255)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_logradouro VARCHAR(150)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_numero VARCHAR(20)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_bairro VARCHAR(80)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_cep VARCHAR(8)",
                "ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_uf VARCHAR(2)",
                "ALTER TABLE servicos ADD COLUMN IF NOT EXISTS codigo_lista_servico VARCHAR(10)",
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
