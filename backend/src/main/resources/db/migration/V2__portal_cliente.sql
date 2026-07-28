-- V2__portal_cliente.sql
-- Adiciona estruturas do portal do cliente (avaliações, vínculo usuário-cliente, etc.).

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuarios_role;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_role
    CHECK (role IN ('ADMIN', 'BARBEIRO', 'ATENDENTE', 'CLIENTE'));

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS usuario_id BIGINT UNIQUE REFERENCES usuarios(id),
    ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id),
    token           VARCHAR(100) NOT NULL UNIQUE,
    expira_em       TIMESTAMP NOT NULL,
    usado           BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_usuario ON password_reset_tokens(usuario_id);

CREATE TABLE IF NOT EXISTS oauth_identities (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    provider            VARCHAR(30) NOT NULL,
    provider_user_id    VARCHAR(150) NOT NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_oauth_provider CHECK (provider IN ('GOOGLE', 'FACEBOOK'))
);

CREATE INDEX IF NOT EXISTS idx_oauth_usuario ON oauth_identities(usuario_id);

CREATE TABLE IF NOT EXISTS servicos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    nome                VARCHAR(150) NOT NULL,
    descricao           VARCHAR(500),
    preco               NUMERIC(10, 2) NOT NULL,
    duracao_minutos     INTEGER NOT NULL DEFAULT 30,
    comissao_percentual NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_servicos_duracao CHECK (duracao_minutos > 0),
    CONSTRAINT ck_servicos_preco CHECK (preco >= 0)
);

CREATE INDEX IF NOT EXISTS idx_servicos_barbearia ON servicos(barbearia_id);

ALTER TABLE agendamentos
    ADD COLUMN IF NOT EXISTS servico_id BIGINT REFERENCES servicos(id);

CREATE TABLE IF NOT EXISTS avaliacoes (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    agendamento_id      BIGINT NOT NULL UNIQUE REFERENCES agendamentos(id),
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id),
    barbeiro_id         BIGINT NOT NULL REFERENCES barbeiros(id),
    nota                INTEGER NOT NULL,
    comentario          VARCHAR(500),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_avaliacoes_nota CHECK (nota BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_avaliacoes_barbeiro ON avaliacoes(barbeiro_id);
CREATE INDEX IF NOT EXISTS idx_avaliacoes_cliente ON avaliacoes(cliente_id);
