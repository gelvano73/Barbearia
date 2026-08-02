-- Segurança: CPF e telefone no usuário + suporte a login alternativo
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_cpf ON usuarios(cpf);
