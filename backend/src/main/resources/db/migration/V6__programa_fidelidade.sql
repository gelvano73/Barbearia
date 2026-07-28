-- V6__programa_fidelidade.sql
-- Cria tabelas do programa de fidelidade (config, saldo e movimentos).

CREATE TABLE fidelidade_config (
    id                      BIGSERIAL PRIMARY KEY,
    barbearia_id            BIGINT NOT NULL UNIQUE REFERENCES barbearias(id),
    pontos_por_atendimento  INT NOT NULL DEFAULT 1,
    pontos_para_resgate     INT NOT NULL DEFAULT 10,
    descricao               VARCHAR(255) NOT NULL DEFAULT 'A cada 10 cortes = 1 grátis',
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fidelidade_pontos_atendimento CHECK (pontos_por_atendimento > 0),
    CONSTRAINT ck_fidelidade_pontos_resgate CHECK (pontos_para_resgate > 0)
);

CREATE TABLE fidelidade_saldos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    cliente_id          BIGINT NOT NULL UNIQUE REFERENCES clientes(id),
    pontos              INT NOT NULL DEFAULT 0,
    pontos_acumulados   INT NOT NULL DEFAULT 0,
    resgates            INT NOT NULL DEFAULT 0,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fidelidade_saldo_pontos CHECK (pontos >= 0),
    CONSTRAINT ck_fidelidade_saldo_acumulados CHECK (pontos_acumulados >= 0),
    CONSTRAINT ck_fidelidade_saldo_resgates CHECK (resgates >= 0)
);

CREATE INDEX idx_fidelidade_saldos_barbearia ON fidelidade_saldos(barbearia_id, pontos DESC);

CREATE TABLE fidelidade_movimentos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id),
    tipo                VARCHAR(20) NOT NULL,
    pontos              INT NOT NULL,
    saldo_apos          INT NOT NULL,
    descricao           VARCHAR(255),
    agendamento_id      BIGINT REFERENCES agendamentos(id),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fidelidade_mov_tipo CHECK (tipo IN ('CREDITO', 'RESGATE', 'AJUSTE')),
    CONSTRAINT ck_fidelidade_mov_pontos CHECK (pontos > 0)
);

CREATE INDEX idx_fidelidade_mov_cliente ON fidelidade_movimentos(cliente_id, criado_em DESC);
CREATE UNIQUE INDEX uq_fidelidade_credito_agendamento
    ON fidelidade_movimentos(agendamento_id)
    WHERE agendamento_id IS NOT NULL AND tipo = 'CREDITO';
