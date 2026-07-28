-- V3__portal_barbeiro.sql
-- Adiciona estruturas do portal do barbeiro (horários, férias, metas, comissões).

CREATE TABLE barbeiro_horarios (
    id              BIGSERIAL PRIMARY KEY,
    barbeiro_id     BIGINT NOT NULL REFERENCES barbeiros(id),
    dia_semana      INTEGER NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fim        TIME NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_barbeiro_horarios_dia CHECK (dia_semana BETWEEN 0 AND 6),
    CONSTRAINT ck_barbeiro_horarios_periodo CHECK (hora_fim > hora_inicio),
    CONSTRAINT uk_barbeiro_horario_dia UNIQUE (barbeiro_id, dia_semana)
);

CREATE INDEX idx_barbeiro_horarios_barbeiro ON barbeiro_horarios(barbeiro_id);

CREATE TABLE barbeiro_ferias (
    id              BIGSERIAL PRIMARY KEY,
    barbeiro_id     BIGINT NOT NULL REFERENCES barbeiros(id),
    data_inicio     DATE NOT NULL,
    data_fim        DATE NOT NULL,
    motivo          VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'SOLICITADO',
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_barbeiro_ferias_periodo CHECK (data_fim >= data_inicio),
    CONSTRAINT ck_barbeiro_ferias_status CHECK (
        status IN ('SOLICITADO', 'APROVADO', 'REJEITADO', 'CANCELADO')
    )
);

CREATE INDEX idx_barbeiro_ferias_barbeiro ON barbeiro_ferias(barbeiro_id);

CREATE TABLE barbeiro_metas (
    id                      BIGSERIAL PRIMARY KEY,
    barbeiro_id             BIGINT NOT NULL REFERENCES barbeiros(id),
    ano                     INTEGER NOT NULL,
    mes                     INTEGER NOT NULL,
    meta_atendimentos       INTEGER NOT NULL DEFAULT 0,
    meta_comissao           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_barbeiro_metas_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT uk_barbeiro_meta_periodo UNIQUE (barbeiro_id, ano, mes)
);

CREATE TABLE comissoes (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    barbeiro_id         BIGINT NOT NULL REFERENCES barbeiros(id),
    agendamento_id      BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),
    valor_servico       NUMERIC(12, 2) NOT NULL,
    percentual          NUMERIC(5, 2) NOT NULL,
    valor_comissao      NUMERIC(12, 2) NOT NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comissoes_barbeiro ON comissoes(barbeiro_id);
CREATE INDEX idx_comissoes_barbeiro_data ON comissoes(barbeiro_id, criado_em);
