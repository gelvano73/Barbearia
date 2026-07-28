-- V7__controle_estoque.sql
-- Cria tabelas de produtos e movimentos de estoque.

CREATE TABLE produtos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    nome                VARCHAR(150) NOT NULL,
    unidade             VARCHAR(30) NOT NULL DEFAULT 'UN',
    quantidade          NUMERIC(12, 3) NOT NULL DEFAULT 0,
    estoque_minimo      NUMERIC(12, 3) NOT NULL DEFAULT 0,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_produtos_quantidade CHECK (quantidade >= 0),
    CONSTRAINT ck_produtos_minimo CHECK (estoque_minimo >= 0)
);

CREATE UNIQUE INDEX uq_produtos_nome_ativo
    ON produtos (barbearia_id, LOWER(nome))
    WHERE ativo = TRUE;

CREATE INDEX idx_produtos_barbearia ON produtos(barbearia_id, ativo);

CREATE TABLE estoque_movimentos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    produto_id          BIGINT NOT NULL REFERENCES produtos(id),
    tipo                VARCHAR(20) NOT NULL,
    quantidade          NUMERIC(12, 3) NOT NULL,
    quantidade_antes    NUMERIC(12, 3) NOT NULL,
    quantidade_depois   NUMERIC(12, 3) NOT NULL,
    observacao          VARCHAR(255),
    usuario_id          BIGINT REFERENCES usuarios(id),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_estoque_mov_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA', 'INVENTARIO')),
    CONSTRAINT ck_estoque_mov_qtd CHECK (quantidade >= 0)
);

CREATE INDEX idx_estoque_mov_produto ON estoque_movimentos(produto_id, criado_em DESC);
CREATE INDEX idx_estoque_mov_barbearia ON estoque_movimentos(barbearia_id, criado_em DESC);
