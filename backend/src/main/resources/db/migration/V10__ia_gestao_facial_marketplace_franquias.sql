-- V10__ia_gestao_facial_marketplace_franquias.sql
-- Adiciona check-in facial, marketplace, empresas/franquias e suporte a IA de gestão.

-- Franquias / multiempresa
CREATE TABLE empresas (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    cnpj            VARCHAR(18) UNIQUE,
    telefone        VARCHAR(20),
    email           VARCHAR(150),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE barbearias
    ADD COLUMN IF NOT EXISTS empresa_id BIGINT REFERENCES empresas(id);

CREATE INDEX IF NOT EXISTS idx_barbearias_empresa ON barbearias(empresa_id);

-- Marketplace: preço e vitrine
ALTER TABLE produtos
    ADD COLUMN IF NOT EXISTS preco NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS descricao_venda VARCHAR(500),
    ADD COLUMN IF NOT EXISTS marketplace_ativo BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE produtos SET preco = 0 WHERE preco IS NULL;
ALTER TABLE produtos ALTER COLUMN preco SET DEFAULT 0;
ALTER TABLE produtos ALTER COLUMN preco SET NOT NULL;

CREATE TABLE pedidos_marketplace (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    unidade_id      BIGINT REFERENCES unidades(id),
    cliente_id      BIGINT REFERENCES clientes(id),
    cliente_nome    VARCHAR(150) NOT NULL,
    cliente_telefone VARCHAR(20) NOT NULL,
    cliente_email   VARCHAR(150),
    endereco_entrega VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    total           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    observacoes     VARCHAR(500),
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pedido_status CHECK (status IN ('PENDENTE', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO')),
    CONSTRAINT ck_pedido_total CHECK (total >= 0)
);

CREATE INDEX idx_pedidos_barbearia ON pedidos_marketplace(barbearia_id, criado_em DESC);

CREATE TABLE pedido_itens (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES pedidos_marketplace(id) ON DELETE CASCADE,
    produto_id      BIGINT NOT NULL REFERENCES produtos(id),
    produto_nome    VARCHAR(150) NOT NULL,
    quantidade      NUMERIC(12, 3) NOT NULL,
    preco_unitario  NUMERIC(12, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_item_qtd CHECK (quantidade > 0)
);

-- Reconhecimento facial / check-in
CREATE TABLE face_perfis (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    cliente_id      BIGINT NOT NULL UNIQUE REFERENCES clientes(id),
    foto_url        VARCHAR(500) NOT NULL,
    assinatura      VARCHAR(128) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_face_barbearia ON face_perfis(barbearia_id, ativo);

CREATE TABLE checkins (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    unidade_id      BIGINT REFERENCES unidades(id),
    cliente_id      BIGINT NOT NULL REFERENCES clientes(id),
    metodo          VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    confianca       NUMERIC(5, 2),
    foto_url        VARCHAR(500),
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_checkin_metodo CHECK (metodo IN ('MANUAL', 'FACIAL', 'WHATSAPP'))
);

CREATE INDEX idx_checkins_barbearia ON checkins(barbearia_id, criado_em DESC);
