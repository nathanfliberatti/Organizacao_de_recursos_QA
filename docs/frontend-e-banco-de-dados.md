# Frontend e banco de dados

## Frontend

SPA em HTML/CSS/JavaScript puro (módulos ES, sem build e sem Node) em `src/main/resources/static`, servida pelo
Spring Boot. Consome somente a API REST; **toda autorização é reaplicada no servidor** (`@PreAuthorize`), a
interface apenas esconde o que o perfil não pode usar (`RF-01`, `RNF-01`). O token JWT fica em `sessionStorage`;
um `401` encerra a sessão. Todo texto dinâmico é escapado (`js/util.js`, tag `html`) e a interface é responsiva
(menu recolhível em telas estreitas), com rótulos nos campos, foco visível e mensagens em região `aria-live`
(`RNF-04`).

| Tela | Perfis | Requisitos | Endpoints |
|---|---|---|---|
| Login | todos | RF-01 | `POST /api/auth/login`, `GET /api/auth/me` |
| Recursos (pesquisa por tipo, capacidade, local, competência, texto e disponibilidade; cadastro/edição) | todos veem; Administrador edita | RF-02, RF-03 | `GET /api/resources`, `GET /api/resources/{id}`, `POST/PUT /api/resources` |
| Reservas (criar, verificar disponibilidade, alterar, cancelar, aprovar, iniciar, concluir, histórico) | todos; aprovar = Responsável; histórico = Responsável/Administrador | RF-04 a RF-07, RF-10, RF-13 a RF-15 | `/api/reservations/**`, `GET /api/resources/{id}/availability` |
| Manutenção | todos veem; Administrador bloqueia/remove | RF-08 | `GET /api/maintenance`, `POST/DELETE /api/resources/{id}/maintenance` |
| Materiais (retirada/devolução) | Responsável, Administrador | RF-09 | `/api/materials/{id}/withdrawals`, `/api/loans/**` |
| Notificações | todos (Responsável/Administrador podem ver de todos) | RF-11 | `GET /api/notifications` |
| Relatório mensal | Responsável, Administrador | RF-12 | `GET /relatorios/utilizacao` (perfil `postgres`) |
| Usuários | Administrador | RF-01 | `GET/POST /api/users` |

## Comportamentos acrescentados à API

- **Ciclo de vida ligado à agenda:** toda reserva criada em `POST /api/reservations` ganha um registro de ciclo de
  vida com id igual ao da reserva (`reservation_lifecycle.reservation_id = reservations.id::text`). Recurso
  **restrito** nasce `SOLICITADA` e aguarda um Responsável (RF-07); recurso não restrito é aprovado
  automaticamente pelo ator `sistema` (a trilha de auditoria registra o motivo).
- **Cancelamento:** `DELETE /api/reservations/{id}` mantém a regra de posse e a proibição em `EM_USO`/`CONCLUIDA`
  (RF-15) e marca a reserva como `CANCELADA`, liberando sala e professor.
- **Reinício da aplicação:** o estado em memória de `ReservationCatalog` é reconstruído a partir da trilha de
  auditoria quando necessário (`ReservationExtras.rehydrate`).
- **Notificações:** entrega simulada (`delivery = SIMULADA`) para criação, alteração e cancelamento (RF-11).

## Modelo de dados (`src/main/resources/schema.sql`, idempotente)

| Tabela | Papel |
|---|---|
| `resources` | Catálogo: `resource_type` (`SALA`/`PROFESSOR`/`MATERIAL`), `capacity`, `location`, `competency`, `restricted` |
| `reservations` | Agenda; `EXCLUDE USING gist` impede sobreposição de sala e de professor (ADR-001) |
| `reservation_conflict_attempts` | Tentativas recusadas (relatório RF-12) |
| `reservation_lifecycle` / `reservation_audit` | Estado atual e trilha auditável (RF-10, RF-14) |
| `app_users` | Usuários, hash BCrypt e perfil |
| `maintenance_periods` | Um período de bloqueio por recurso (RF-08) |
| `material_loans` | Retirada/devolução; índice único parcial garante no máximo uma retirada em aberto por material (RF-09) |
| `notifications` | Eventos notificados (RF-11) |

## Lacunas conhecidas (decisões ainda em aberto na documentação)

- ~~Estados `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` do ciclo de vida (RF-14) não foram modelados~~ **(resolvido na entrega final: [arquitetura-implementada.md](arquitetura-implementada.md#31-máquina-de-estados-da-reserva-rf-14)).** Texto original: a matriz de
  transições e de perfis ainda precisa ser definida. O cancelamento hoje é a exclusão lógica já existente.
- O fuso para fechar o mês do relatório é UTC (a tela informa isso); a decisão de negócio continua pendente.
- ~~O armazenamento em memória não detecta sobreposição de horários~~ **(resolvido: DEF-014)**. O modo em memória vale para uma única instância e perde os dados ao parar.
- Manutenção cadastrada não afeta reservas já existentes (decisão pendente em `requisitos-funcionais.md`).
- Os testes `ApplicationFlowIT` e os demais `*IT` exigem Docker (Testcontainers).
