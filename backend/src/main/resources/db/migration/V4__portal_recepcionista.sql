-- V4__portal_recepcionista.sql
-- Adiciona estruturas do portal da recepção (fila, caixa e movimentos).

CREATE TABLE fila_atendimento (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id),
    barbeiro_id         BIGINT REFERENCES barbeiros(id),
    servico_id          BIGINT REFERENCES servicos(id),
    posicao             INTEGER NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO',
    prioridade          BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes         VARCHAR(500),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fila_status CHECK (
        status IN ('AGUARDANDO', 'EM_ATENDIMENTO', 'FINALIZADO', 'CANCELADO', 'DESISTIU')
    )
);

CREATE INDEX idx_fila_barbearia_status ON fila_atendimento(barbearia_id, status);
CREATE INDEX idx_fila_barbearia_posicao ON fila_atendimento(barbearia_id, posicao);

CREATE TABLE caixas (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    aberto_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechado_em          TIMESTAMP,
    valor_abertura      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    valor_informado_fechamento NUMERIC(12, 2),
    status              VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    observacoes         VARCHAR(500),
    CONSTRAINT ck_caixa_status CHECK (status IN ('ABERTO', 'FECHADO'))
);

CREATE INDEX idx_caixas_barbearia ON caixas(barbearia_id, status);

CREATE TABLE pagamentos (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias(id),
    caixa_id            BIGINT REFERENCES caixas(id),
    agendamento_id      BIGINT REFERENCES agendamentos(id),
    cliente_id          BIGINT REFERENCES clientes(id),
    valor               NUMERIC(12, 2) NOT NULL,
    forma_pagamento     VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PAGO',
    descricao           VARCHAR(255),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pagamentos_forma CHECK (
        forma_pagamento IN ('PIX', 'DINHEIRO', 'CREDITO', 'DEBITO')
    ),
    CONSTRAINT ck_pagamentos_status CHECK (
        status IN ('PAGO', 'PENDENTE', 'CANCELADO')
    ),
    CONSTRAINT ck_pagamentos_valor CHECK (valor > 0)
);

CREATE INDEX idx_pagamentos_caixa ON pagamentos(caixa_id);
CREATE INDEX idx_pagamentos_barbearia ON pagamentos(barbearia_id, criado_em);

CREATE TABLE movimentos_caixa (
    id                  BIGSERIAL PRIMARY KEY,
    caixa_id            BIGINT NOT NULL REFERENCES caixas(id),
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    tipo                VARCHAR(20) NOT NULL,
    forma_pagamento     VARCHAR(20),
    valor               NUMERIC(12, 2) NOT NULL,
    descricao           VARCHAR(255),
    pagamento_id        BIGINT REFERENCES pagamentos(id),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movimentos_tipo CHECK (
        tipo IN ('ENTRADA', 'SAIDA', 'SANGRIA', 'SUPRIMENTO')
    ),
    CONSTRAINT ck_movimentos_valor CHECK (valor > 0)
);

CREATE INDEX idx_movimentos_caixa ON movimentos_caixa(caixa_id);
