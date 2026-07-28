# Barbearia SaaS

Sistema multi-tenant para gestão de barbearias com **Java 21**, **Spring Boot 3**, **PostgreSQL**, **React + TypeScript + Tailwind**, **JWT** e **Docker**.

## Arquitetura

```
┌─────────────┐     JWT      ┌──────────────────┐     JPA      ┌────────────┐
│  React+TS   │ ───────────► │  Spring Boot 3   │ ───────────► │ PostgreSQL │
│  Tailwind   │ ◄─────────── │  Java 21 /api/*  │ ◄─────────── │            │
└─────────────┘              └──────────────────┘              └────────────┘
                                     │
                              Flyway migrations
                              Swagger (OpenAPI)
```

### Multi-tenant
Cada barbearia é um tenant. Clientes, barbeiros e agendamentos são isolados por `barbearia_id` via JWT.

### Módulos iniciais
| Módulo | Descrição |
|--------|-----------|
| Auth | Registro de barbearia + login JWT |
| Clientes | CRUD com soft delete |
| Barbeiros | CRUD com soft delete |
| Serviços | CRUD com soft delete (preço, duração, comissão) |
| Agendamentos | Agenda inteligente: slots livres, serviço, duração automática, sem conflito |
| Pagamentos | PIX, crédito, débito e dinheiro (valor, data, cliente, serviço) |
| Fidelidade | Pontos, resgates e histórico (ex.: a cada 10 cortes = 1 grátis) |
| Estoque | Produtos (gel, pomada…) com entrada, saída e inventário |
| Caixa | Abrir, fechar, sangria e suprimento |
| Comissões | Por barbeiro, mensal e ranking (geradas ao concluir atendimento) |
| Relatórios | Faturamento D/S/M, serviços, clientes frequentes e lucro líquido |
| Unidades | Filiais/lojas da barbearia (`unidades`) |
| WhatsApp IA | Agendamento e respostas automáticas via webhook / simulador |
| IA Gestão | Previsão de faturamento e estoque |
| Check-in facial | Cadastro de face + check-in do cliente |
| Marketplace | Venda online de produtos (`/loja/:id`) |
| Franquias | Multiempresa + multiunidade |

## Estrutura do projeto

```
Barbearia/
├── docker-compose.yml
├── backend/                 # Spring Boot 3.5 + Java 21
│   ├── src/main/java/...
│   └── src/test/java/...
└── frontend/                # React + Vite
```

## Como rodar

### Opção 1 — Docker Compose (recomendado)

```bash
docker compose up --build
```

| Serviço   | URL |
|-----------|-----|
| Frontend  | http://localhost:3000 |
| API       | http://localhost:8080 |
| Swagger   | http://localhost:8080/swagger-ui.html |
| PostgreSQL| localhost:5432 |

### Opção 2 — Desenvolvimento local

**Banco**
```bash
docker compose up postgres -d
```

**Backend**
```bash
cd backend
# com Maven instalado:
mvn spring-boot:run
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```
Frontend em http://localhost:5173 (proxy `/api` → `:8080`).

## APIs principais

### Auth
- `POST /api/auth/registro` — cria barbearia + admin
- `POST /api/auth/login` — retorna JWT

### Clientes (`Authorization: Bearer <token>`)
- `GET/POST /api/clientes`
- `GET/PUT/DELETE /api/clientes/{id}`

### Barbeiros
- `GET/POST /api/barbeiros`
- `GET/PUT/DELETE /api/barbeiros/{id}`

### Serviços
- `GET/POST /api/servicos`
- `GET/PUT/DELETE /api/servicos/{id}`

### Pagamentos
- `GET /api/pagamentos?data=2026-07-17`
- `POST /api/pagamentos`
- `DELETE /api/pagamentos/{id}` (cancela)

### Fidelidade
- `GET/PUT /api/fidelidade/config`
- `GET /api/fidelidade/saldos`
- `GET /api/fidelidade/saldos/{clienteId}/historico`
- `POST /api/fidelidade/resgatar`
- `GET /api/portal/fidelidade` (cliente)

### Estoque
- `GET/POST /api/estoque/produtos`
- `PUT/DELETE /api/estoque/produtos/{id}`
- `GET/POST /api/estoque/movimentos` (ENTRADA, SAIDA, INVENTARIO)

### Caixa diário
- `GET /api/caixa`
- `POST /api/caixa/abrir`
- `POST /api/caixa/fechar`
- `POST /api/caixa/sangria`
- `POST /api/caixa/suprimento`
- Também em `/api/recepcao/caixa/**`

### Comissões
- `GET /api/comissoes?ano=&mes=&barbeiroId=` (detalhe por atendimento)
- `GET /api/comissoes/mensal?ano=&mes=` (totais + ranking)

### Relatórios
- `GET /api/relatorios?periodo=DIARIO|SEMANAL|MENSAL&data=YYYY-MM-DD`
  (faturamento por dia, serviços mais vendidos, clientes frequentes, lucro líquido)

### Unidades
- `GET/POST /api/unidades`
- `PUT/DELETE /api/unidades/{id}`

Documentação do schema: [`backend/docs/DATABASE.md`](backend/docs/DATABASE.md)

### Agendamentos
- `GET /api/agendamentos?data=2026-07-17&barbeiroId=1`
- `GET /api/agendamentos/horarios-disponiveis?barbeiroId=1&servicoId=1&data=2026-07-18`
- `POST /api/agendamentos`
- `PUT /api/agendamentos/{id}`
- `PATCH /api/agendamentos/{id}/status`
- `DELETE /api/agendamentos/{id}` (cancela)

## Portal do Cliente (MVP)

Área do cliente em `/portal`:

- Cadastro e login (`/api/auth/cliente/*`)
- Recuperação de senha (token em modo dev)
- OAuth Google/Facebook em modo desenvolvimento
- Foto de perfil
- Agendar / cancelar / reagendar
- Histórico e avaliação de barbeiros

Acesse: http://localhost:5173/portal/login

## Portal do Barbeiro

Área do barbeiro em `/barbeiro`:

- Login (`/api/auth/barbeiro/login`) — conta criada pelo admin em Barbeiros → Criar conta
- Dashboard pessoal (agenda do dia, comissão, avaliações, meta)
- Agenda própria + atualização de status (gera comissão ao concluir)
- Gestão de horários semanais
- Controle de férias
- Histórico de atendimentos
- Comissões automáticas
- Avaliações recebidas
- Meta mensal

Admin (`/comissoes`): comissão por barbeiro, consolidado mensal e ranking.

Acesse: http://localhost:5173/barbeiro/login

## Portal da Recepcionista

Área da recepção em `/recepcao`:

- Login (`/api/auth/recepcao/login`) — conta criada pelo admin no Dashboard → Criar recepcionista
- Cadastro e edição de clientes
- Agendamento manual
- Controle de fila (walk-in, prioridade, status)
- Registro de pagamentos (PIX, dinheiro, cartão)
- Caixa diário (abrir/fechar, sangria, suprimento)

APIs em `/api/recepcao/**` (roles `ATENDENTE` e `ADMIN`).

Acesse: http://localhost:5173/recepcao/login

## IA de Atendimento

Assistente no portal do cliente (`/portal/assistente`) e no **WhatsApp**:

- Respostas automáticas em português (saudações, dúvidas, ajuda)
- Sugere serviços (corte, barba, combo…)
- Lista barbeiros e horários livres (9h–18h, seg–sáb)
- Agenda com confirmação (“sim”) — gera agendamento real

| Canal | Endpoint |
|-------|----------|
| Portal (JWT cliente) | `POST /api/portal/ia/chat` |
| WhatsApp (Meta) | `GET/POST /api/webhooks/whatsapp` |
| Simulador admin | `POST /api/whatsapp/simular` + UI `/whatsapp` |

Motor rule-based (`app.ai.provider=rule-engine`). Sem token da Cloud API, o envio é **simulado** (log). Variáveis: `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_BARBEARIA_ID`.

## IA de Gestão

`GET /api/gestao/previsoes` · UI `/gestao`

- Previsão de faturamento (7 e 30 dias) com base no histórico
- Previsão de estoque (consumo médio e risco de ruptura)

## Check-in facial

UI `/checkin` · `POST /api/checkin/facial` · cadastro `POST /api/checkin/face/{clienteId}`

MVP local com assinatura de imagem (sem API externa de face).

## Marketplace

- Vitrine pública: `/loja/{barbeariaId}` · `GET/POST /api/public/marketplace/...`
- Admin pedidos: `/marketplace`
- Ative preço + flag marketplace no cadastro de produtos (Estoque)

## Franquias (multiempresa / multiunidade)

UI `/franquias` · empresas + vínculo da barbearia · unidades em `/unidades`

## Requisitos Não Funcionais

| Requisito | Status | Como |
|-----------|--------|------|
| **JWT** | OK | `JwtService` + filtro; secret via `JWT_SECRET` |
| **BCrypt** | OK | `BCryptPasswordEncoder` no login/registro |
| **API REST** | OK | Endpoints `/api/**` (Spring MVC) |
| **Swagger** | OK | http://localhost:8080/swagger-ui.html (Bearer JWT) |
| **Docker** | OK | `docker compose up --build` (postgres, backend, frontend, backup) |
| **Logs** | OK | Console + arquivo rotativo (`logs/`), `X-Request-Id` / MDC |
| **Backup automático** | OK | Sidecar Docker diário (`pg_dump` + uploads) + job Spring 03:00 + `POST /api/backup/executar` (admin) |

### Backup

- **Docker:** serviço `barbearia-backup` roda `scripts/backup.sh` a cada 24h → volume `backup_data`
- **Local:** agendado às 03:00 (cópia H2/`data` + `uploads` em `backups/`)
- **Manual:** `POST /api/backup/executar` (role ADMIN)
- Retenção padrão: **7 dias** (`BACKUP_RETENTION_DAYS`)

### Logs

- Arquivo: `logs/barbearia-saas.log` (rotação diária/tamanho)
- Header de correlação: `X-Request-Id`

## Roadmap (próximas fases)

Cashback, push e chat — planejados para evolução.

## Exemplo de registro

```json
POST /api/auth/registro
{
  "nomeBarbearia": "Barba Fina",
  "nomeAdmin": "Carlos Admin",
  "email": "admin@barbafina.com",
  "senha": "senha123",
  "telefoneBarbearia": "11999999999"
}
```

```json
POST /api/auth/cliente/registro
{
  "barbeariaId": 1,
  "nome": "João Cliente",
  "telefone": "11988887777",
  "email": "joao@email.com",
  "senha": "senha123"
}
```

## Testes

```bash
cd backend
mvn test
```

Cobertura incluída:
- `ClienteServiceTest` — criação, duplicidade e soft delete
- `ServicoServiceTest` — criação, nome duplicado e soft delete
- `HorarioDisponivelServiceTest` — slots livres da agenda inteligente
- `PagamentoServiceTest` — registro com cliente, serviço, data e forma
- `FidelidadeServiceTest` — crédito, resgate e pontos insuficientes
- `EstoqueServiceTest` — entrada, saída insuficiente e inventário
- `CaixaServiceTest` — abrir, duplicidade e sangria maior que saldo
- `AgendamentoServiceTest` — conflito de horário
- `ClienteControllerTest` — validação e endpoints
- `JwtServiceTest` — geração/validação de token

## Stack

| Camada | Tecnologias |
|--------|-------------|
| **Backend** | Java 21, Spring Boot 3.5.16, Spring Security, JWT (JJWT 0.13), JPA/Hibernate, Flyway, springdoc 2.8 (Swagger) |
| **Banco** | PostgreSQL 16 (prod/Docker); H2 em profile `local` |
| **Front-end** | React 18, TypeScript, Tailwind CSS, Vite, React Router, Axios |
| **Infra** | Docker Compose, Nginx (frontend), backup automático |

## Roadmap (próximas fases)
