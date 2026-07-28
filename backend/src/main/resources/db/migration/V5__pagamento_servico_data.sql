-- V5__pagamento_servico_data.sql
-- Evolui pagamentos e dados de serviço/agendamento relacionados a cobrança.

ALTER TABLE pagamentos
    ADD COLUMN IF NOT EXISTS servico_id BIGINT REFERENCES servicos(id),
    ADD COLUMN IF NOT EXISTS data_pagamento DATE;

UPDATE pagamentos
SET data_pagamento = CAST(criado_em AS DATE)
WHERE data_pagamento IS NULL;

ALTER TABLE pagamentos
    ALTER COLUMN data_pagamento SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pagamentos_data
    ON pagamentos(barbearia_id, data_pagamento);
