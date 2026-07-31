-- Produção: gateway, assinatura SaaS, metadados de pagamento online
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS plano VARCHAR(30) DEFAULT 'TRIAL';
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS assinatura_status VARCHAR(30) DEFAULT 'ATIVA';
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS assinatura_vence_em TIMESTAMP;
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS mp_customer_id VARCHAR(80);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS mp_subscription_id VARCHAR(80);

ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway VARCHAR(30);
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(120);
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS gateway_status VARCHAR(60);
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(500);
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS pix_qr_code TEXT;
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS pix_copia_cola TEXT;

CREATE TABLE IF NOT EXISTS notificacao_log (
    id BIGSERIAL PRIMARY KEY,
    barbearia_id BIGINT NOT NULL,
    canal VARCHAR(20) NOT NULL,
    destino VARCHAR(150),
    assunto VARCHAR(200),
    corpo TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ENVIADO',
    referencia VARCHAR(120),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacao_barbearia ON notificacao_log(barbearia_id);
