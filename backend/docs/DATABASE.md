# Banco de dados — Barbearia SaaS

Schema multi-tenant (PostgreSQL via Flyway). Tenant = `barbearias`. Filiais = `unidades`.

## Tabelas principais (catálogo)

| Nome pedido | Implementação |
|-------------|---------------|
| `usuarios` | tabela `usuarios` |
| `clientes` | tabela `clientes` |
| `barbeiros` | tabela `barbeiros` |
| `servicos` | tabela `servicos` |
| `agendamentos` | tabela `agendamentos` |
| `pagamentos` | tabela `pagamentos` |
| `produtos` | tabela `produtos` |
| `estoque` | view `estoque` ← `produtos` (+ histórico em `estoque_movimentos`) |
| `fidelidade` | view `fidelidade` ← `fidelidade_saldos` (+ `fidelidade_config`, `fidelidade_movimentos`) |
| `comissoes` | tabela `comissoes` |
| `caixa` | view `caixa` ← `caixas` (+ `movimentos_caixa`) |
| `unidades` | tabela `unidades` (filiais/lojas) |

## Diagrama (núcleo)

```mermaid
erDiagram
    barbearias ||--o{ unidades : tem
    barbearias ||--o{ usuarios : tem
    barbearias ||--o{ clientes : tem
    barbearias ||--o{ barbeiros : tem
    barbearias ||--o{ servicos : tem
    barbearias ||--o{ agendamentos : tem
    barbearias ||--o{ pagamentos : tem
    barbearias ||--o{ produtos : tem
    barbearias ||--o{ comissoes : tem
    barbearias ||--o{ caixas : tem
    unidades ||--o{ agendamentos : atende
    unidades ||--o{ produtos : estoque
    unidades ||--o{ caixas : opera
    clientes ||--o{ agendamentos : agenda
    barbeiros ||--o{ agendamentos : realiza
    servicos ||--o{ agendamentos : usa
    agendamentos ||--o| comissoes : gera
    caixas ||--o{ pagamentos : registra
    produtos ||--o{ estoque_movimentos : movimenta
```

## Migrations Flyway

| Versão | Conteúdo |
|--------|----------|
| V1 | barbearias, usuarios, clientes, barbeiros, agendamentos |
| V2 | portal cliente, servicos, avaliacoes |
| V3 | portal barbeiro, comissoes |
| V4 | recepção, caixas, pagamentos, movimentos_caixa |
| V5 | pagamento + serviço + data |
| V6 | fidelidade_* |
| V7 | produtos, estoque_movimentos |
| V8 | unidades, FKs `unidade_id`, views estoque/fidelidade/caixa |

## Ambientes

- **Produção / default:** PostgreSQL, `flyway.enabled=true`, `ddl-auto=validate`
- **Local (`local`):** H2 file, Flyway off, `ddl-auto=update` (Hibernate cria/atualiza entidades, inclusive `unidades`)

## API de unidades

- `GET/POST /api/unidades`
- `GET/PUT/DELETE /api/unidades/{id}`

No cadastro da barbearia, a unidade **Matriz** é criada automaticamente.
