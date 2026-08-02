-- NFS-e: dados fiscais do emitente, CPF do tomador e notas emitidas

ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS razao_social VARCHAR(150);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS inscricao_municipal VARCHAR(30);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS codigo_municipio_ibge VARCHAR(7);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS aliquota_iss DECIMAL(5, 2) DEFAULT 0;
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS codigo_servico_padrao VARCHAR(10) DEFAULT '6.02';
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS regime_tributario VARCHAR(30) DEFAULT 'SIMPLES_NACIONAL';
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS optante_simples BOOLEAN DEFAULT TRUE;
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS nfse_habilitada BOOLEAN DEFAULT FALSE;
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS nfse_token VARCHAR(255);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_logradouro VARCHAR(150);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_numero VARCHAR(20);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_bairro VARCHAR(80);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_cep VARCHAR(8);
ALTER TABLE barbearias ADD COLUMN IF NOT EXISTS endereco_uf VARCHAR(2);

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);

ALTER TABLE servicos ADD COLUMN IF NOT EXISTS codigo_lista_servico VARCHAR(10);

CREATE TABLE IF NOT EXISTS notas_fiscais (
    id                  BIGSERIAL PRIMARY KEY,
    barbearia_id        BIGINT NOT NULL REFERENCES barbearias (id),
    pagamento_id        BIGINT NOT NULL UNIQUE REFERENCES pagamentos (id),
    cliente_id          BIGINT REFERENCES clientes (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    provedor            VARCHAR(40) NOT NULL DEFAULT 'FOCUS_NFE',
    referencia_externa  VARCHAR(80) NOT NULL,
    numero              VARCHAR(40),
    codigo_verificacao  VARCHAR(80),
    url_pdf             VARCHAR(500),
    url_xml             VARCHAR(500),
    tomador_cpf         VARCHAR(11) NOT NULL,
    tomador_nome        VARCHAR(150) NOT NULL,
    valor_servicos      DECIMAL(12, 2) NOT NULL,
    aliquota_iss        DECIMAL(5, 2),
    codigo_servico      VARCHAR(10),
    discriminacao       VARCHAR(1000),
    mensagem_erro       VARCHAR(1000),
    resposta_json       TEXT,
    emitido_em          TIMESTAMP,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notas_fiscais_barbearia ON notas_fiscais (barbearia_id);
CREATE INDEX IF NOT EXISTS idx_notas_fiscais_status ON notas_fiscais (status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_clientes_barbearia_cpf
    ON clientes (barbearia_id, cpf) WHERE cpf IS NOT NULL;
