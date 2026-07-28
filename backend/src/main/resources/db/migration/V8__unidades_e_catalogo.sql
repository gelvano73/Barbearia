-- V8__unidades_e_catalogo.sql
-- Adiciona unidades físicas e ajustes de catálogo multi-unidade.

CREATE TABLE unidades (
    id              BIGSERIAL PRIMARY KEY,
    barbearia_id    BIGINT NOT NULL REFERENCES barbearias(id),
    nome            VARCHAR(150) NOT NULL,
    endereco        VARCHAR(255),
    telefone        VARCHAR(20),
    padrao          BOOLEAN NOT NULL DEFAULT FALSE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_unidades_barbearia ON unidades(barbearia_id, ativo);
CREATE UNIQUE INDEX uq_unidades_nome_ativo
    ON unidades (barbearia_id, LOWER(nome))
    WHERE ativo = TRUE;
CREATE UNIQUE INDEX uq_unidades_padrao
    ON unidades (barbearia_id)
    WHERE padrao = TRUE AND ativo = TRUE;

-- Uma unidade Matriz por barbearia existente
INSERT INTO unidades (barbearia_id, nome, endereco, telefone, padrao, ativo)
SELECT b.id, 'Matriz', b.endereco, b.telefone, TRUE, TRUE
FROM barbearias b
WHERE NOT EXISTS (
    SELECT 1 FROM unidades u WHERE u.barbearia_id = b.id
);

ALTER TABLE produtos
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);
ALTER TABLE caixas
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);
ALTER TABLE agendamentos
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);
ALTER TABLE barbeiros
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);
ALTER TABLE estoque_movimentos
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);
ALTER TABLE pagamentos
    ADD COLUMN IF NOT EXISTS unidade_id BIGINT REFERENCES unidades(id);

UPDATE produtos p
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = p.barbearia_id AND u.padrao = TRUE AND p.unidade_id IS NULL;

UPDATE caixas c
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = c.barbearia_id AND u.padrao = TRUE AND c.unidade_id IS NULL;

UPDATE agendamentos a
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = a.barbearia_id AND u.padrao = TRUE AND a.unidade_id IS NULL;

UPDATE barbeiros b
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = b.barbearia_id AND u.padrao = TRUE AND b.unidade_id IS NULL;

UPDATE estoque_movimentos e
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = e.barbearia_id AND u.padrao = TRUE AND e.unidade_id IS NULL;

UPDATE pagamentos p
SET unidade_id = u.id
FROM unidades u
WHERE u.barbearia_id = p.barbearia_id AND u.padrao = TRUE AND p.unidade_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_produtos_unidade ON produtos(unidade_id);
CREATE INDEX IF NOT EXISTS idx_caixas_unidade ON caixas(unidade_id);
CREATE INDEX IF NOT EXISTS idx_agendamentos_unidade ON agendamentos(unidade_id);
CREATE INDEX IF NOT EXISTS idx_pagamentos_unidade ON pagamentos(unidade_id);

-- Views com os nomes do catálogo funcional (consultas / BI)
CREATE OR REPLACE VIEW estoque AS
SELECT
    p.id,
    p.barbearia_id,
    p.unidade_id,
    p.nome AS produto,
    p.unidade AS unidade_medida,
    p.quantidade,
    p.estoque_minimo,
    p.ativo,
    p.criado_em,
    p.atualizado_em
FROM produtos p;

CREATE OR REPLACE VIEW fidelidade AS
SELECT
    s.id,
    s.barbearia_id,
    s.cliente_id,
    s.pontos,
    s.pontos_acumulados,
    s.resgates,
    s.atualizado_em
FROM fidelidade_saldos s;

CREATE OR REPLACE VIEW caixa AS
SELECT
    c.id,
    c.barbearia_id,
    c.unidade_id,
    c.usuario_id,
    c.aberto_em,
    c.fechado_em,
    c.valor_abertura,
    c.valor_informado_fechamento,
    c.status,
    c.observacoes
FROM caixas c;

COMMENT ON TABLE unidades IS 'Filiais/lojas físicas da barbearia (tenant = barbearias)';
COMMENT ON VIEW estoque IS 'Catálogo: saldo de produtos (tabela física produtos + estoque_movimentos)';
COMMENT ON VIEW fidelidade IS 'Catálogo: saldos do programa (tabelas fidelidade_*)';
COMMENT ON VIEW caixa IS 'Catálogo: caixas diários (tabela física caixas + movimentos_caixa)';
COMMENT ON TABLE usuarios IS 'Contas de acesso (ADMIN, ATENDENTE, BARBEIRO, CLIENTE)';
COMMENT ON TABLE clientes IS 'Clientes da barbearia';
COMMENT ON TABLE barbeiros IS 'Profissionais';
COMMENT ON TABLE servicos IS 'Catálogo de serviços e comissão percentual';
COMMENT ON TABLE agendamentos IS 'Agenda de atendimentos';
COMMENT ON TABLE pagamentos IS 'Recebimentos (faturamento)';
COMMENT ON TABLE produtos IS 'Itens de estoque';
COMMENT ON TABLE comissoes IS 'Comissões geradas ao concluir agendamento';
