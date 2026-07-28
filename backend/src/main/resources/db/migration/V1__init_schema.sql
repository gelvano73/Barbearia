-- V1__init_schema.sql
-- Schema multi-tenant para SaaS de barbearias

CREATE TABLE barbearias (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    cnpj            VARCHAR(18) UNIQUE,
    telefone        VARCHAR(20),
    email           VARCHAR(150),
    endereco        VARCHAR(255),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    senha_hash      VARCHAR(255) NOT NULL,
    role            VARCHAR(30) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_role CHECK (role IN ('ADMIN', 'BARBEIRO', 'ATENDENTE'))
);

CREATE INDEX idx_usuarios_barbearia ON usuarios(barbearia_id);

CREATE TABLE clientes (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    nome            VARCHAR(150) NOT NULL,
    telefone        VARCHAR(20) NOT NULL,
    email           VARCHAR(150),
    observacoes     VARCHAR(500),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clientes_barbearia ON clientes(barbearia_id);
CREATE INDEX idx_clientes_telefone ON clientes(barbearia_id, telefone);

CREATE TABLE barbeiros (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    usuario_id      BIGINT UNIQUE REFERENCES usuarios(id),
    nome            VARCHAR(150) NOT NULL,
    telefone        VARCHAR(20),
    especialidade   VARCHAR(150),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_barbeiros_barbearia ON barbeiros(barbearia_id);

CREATE TABLE agendamentos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id),
    barbeiro_id         BIGINT NOT NULL REFERENCES barbeiros(id),
    data_hora           TIMESTAMP NOT NULL,
    duracao_minutos     INTEGER NOT NULL DEFAULT 30,
    status              VARCHAR(30) NOT NULL DEFAULT 'AGENDADO',
    servico             VARCHAR(150),
    observacoes         VARCHAR(500),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agendamentos_status CHECK (
        status IN ('AGENDADO', 'CONFIRMADO', 'EM_ATENDIMENTO', 'CONCLUIDO', 'CANCELADO', 'NAO_COMPARECEU')
    ),
    CONSTRAINT ck_agendamentos_duracao CHECK (duracao_minutos > 0)
);

CREATE INDEX idx_agendamentos_barbearia ON agendamentos(barbearia_id);
CREATE INDEX idx_agendamentos_barbeiro_data ON agendamentos(barbeiro_id, data_hora);
CREATE INDEX idx_agendamentos_cliente ON agendamentos(cliente_id);
CREATE INDEX idx_agendamentos_status ON agendamentos(barbearia_id, status);
