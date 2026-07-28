-- V9__whatsapp_sessoes.sql
-- Cria a tabela de sessões de atendimento via WhatsApp.

CREATE TABLE whatsapp_sessoes (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    telefone        VARCHAR(20) NOT NULL,
    cliente_id      BIGINT REFERENCES clientes(id),
    contexto_json   TEXT,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_whatsapp_sessao UNIQUE (barbearia_id, telefone)
);

CREATE INDEX idx_whatsapp_sessoes_telefone ON whatsapp_sessoes(telefone);
