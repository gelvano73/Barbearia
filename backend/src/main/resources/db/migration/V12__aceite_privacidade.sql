-- V12__aceite_privacidade.sql
-- Registra data/hora do aceite da Política de Privacidade (LGPD).

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS aceite_privacidade_em TIMESTAMP;

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS aceite_privacidade_em TIMESTAMP;
