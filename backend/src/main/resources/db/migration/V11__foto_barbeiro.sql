-- V11__foto_barbeiro.sql
-- Adiciona coluna de foto de perfil do barbeiro.

ALTER TABLE barbeiros
    ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);
