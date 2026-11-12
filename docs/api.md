# Documentação da API

API REST em JSON, servida em `http://localhost:8080`. Datas e horas são instantes ISO-8601 em UTC (`2027-03-01T10:00:00Z`). Erros de validação e de regra de negócio usam `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`) sem expor detalhes internos.

## Autenticação (RF-01)

Todas as rotas, exceto `POST /api/auth/login` e os arquivos estáticos do frontend, exigem o cabeçalho `Authorization: Bearer <token>`. O token é um JWT HS256 (claim `roles` com o perfil, `sub` com o usuário) válido por 8 h (`app.security.token-lifetime-seconds`). Sem token ou com token inválido a resposta é `401`; com perfil insuficiente, `403`. A API é stateless (sem cookie nem sessão); por isso o CSRF não se aplica a `/api/**`.

Perfis: `SOLICITANTE`, `RESPONSAVEL`, `ADMINISTRADOR` (matriz completa em [seguranca/matriz-autorizacao.md](seguranca/matriz-autorizacao.md)).

| Método e caminho | Perfil | Corpo / parâmetros | Resposta |
|---|---|---|---|
| `POST /api/auth/login` | público | `{"username","password"}` | `200` `{access_token, token_type, expires_in, user{username,displayName,role}}`; `401` credenciais inválidas; `400` campo ausente |
| `GET /api/auth/me` | autenticado | — | `200` `{username, displayName, role}` |
| `GET /api/users` | ADMINISTRADOR | — | `200` lista de `{username, displayName, role}` |
| `POST /api/users` | ADMINISTRADOR | `{username, password (mín. 8), displayName, role}` | `201`; `409` usuário já existe; `400` validação |

## Recursos (RF-02, RF-03)

Tipos: `SALA`, `PROFESSOR`, `MATERIAL`. Objeto: `{id, name, type, capacity, location, competency, restricted}`.

| Método e caminho | Perfil | Corpo / parâmetros | Resposta |
|---|---|---|---|
| `GET /api/resources` | autenticado | filtros opcionais: `type`, `minCapacity`, `location`, `competency`, `text` (id ou nome), `availableFrom` + `availableTo` (juntos; `availableFrom < availableTo`) | `200` lista; `400` filtros de disponibilidade incompletos ou invertidos |
| `GET /api/resources/{id}` | autenticado | — | `200`; `404` |
| `POST /api/resources` | ADMINISTRADOR | `{id, capacity > 0, name?, type?, location?, competency?, restricted?}` | `201`; `409` id já existe; `400` validação |
| `PUT /api/resources/{id}` | ADMINISTRADOR | mesmo corpo do `POST` (o `id` vem do caminho) | `200`; `404`; `400` |
| `GET /api/resources/{id}/availability` | autenticado | `startsAt`, `endsAt`, `professorId?` | `200` `{available}` (considera reservas, agenda do professor e manutenção); `400` intervalo inválido |

## Manutenção (RF-08)

Um período de bloqueio por recurso; ele torna o recurso indisponível no intervalo.

| Método e caminho | Perfil | Corpo | Resposta |
|---|---|---|---|
| `POST /api/resources/{id}/maintenance` | ADMINISTRADOR | `{startsAt, endsAt}` (`startsAt < endsAt`) | `201`; `400` intervalo inválido |
| `DELETE /api/resources/{id}/maintenance` | ADMINISTRADOR | — | `204`; `404` |
| `GET /api/maintenance` | autenticado | — | `200` lista `{resourceId, startsAt, endsAt}` |

## Reservas (RF-04 a RF-07, RF-10, RF-13 a RF-15)

Toda reserva criada é `SOLICITADA`. Em recurso **restrito** ela só avança após a aprovação de um `RESPONSAVEL` diferente do solicitante (RF-07); em recurso não restrito o sistema aprova automaticamente (ator `sistema`).

**Estados e transições (RF-14).** Toda transição gera um registro de auditoria (RF-10).

| De | Para | Quem pode |
|---|---|---|
| `SOLICITADA` | `APROVADA` | Responsável (nunca o próprio solicitante) |
| `SOLICITADA` | `REJEITADA` | Responsável (nunca o próprio solicitante) |
| `SOLICITADA`, `APROVADA` | `CANCELADA` | dono, Responsável ou Administrador |
| `APROVADA` | `EM_USO` | dono, Responsável ou Administrador |
| `APROVADA` | `NAO_COMPARECEU` | Responsável ou Administrador |
| `EM_USO` | `CONCLUIDA` | dono, Responsável ou Administrador |

`CONCLUIDA`, `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` são estados finais. `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` **devolvem o horário à agenda** (a sala e o professor ficam livres). Uma reserva iniciada (`EM_USO` ou `CONCLUIDA`) **não pode ser cancelada nem apagada** (RF-15, `409`), e o registro permanece consultável. Criação, alteração e cancelamento geram uma notificação (RF-11); rejeição e não comparecimento geram uma notificação do tipo `ALTERACAO`.

| Método e caminho | Perfil | Corpo / parâmetros | Resposta |
|---|---|---|---|
| `POST /api/reservations` | autenticado | `{resourceId, startsAt, endsAt, professorId?}` (professor padrão = solicitante) | `201` `{accepted:true, reservationId}`; `409` `{accepted:false}` por sobreposição de sala/professor ou manutenção; `400` intervalo inválido (término ≤ início) |
| `GET /api/reservations` | autenticado | `mine` (bool) | `200` lista de `{id, resourceId, professorId, requesterId, startsAt, endsAt, schedulingStatus (AGENDADA = ocupa o horário, CANCELADA = horário liberado), lifecycleStatus (estado do ciclo de vida)}`; SOLICITANTE vê só as próprias |
| `GET /api/reservations/{id}` | autenticado | — | `200` `{id, status, approvalDecision, transitions[]}`; `404` |
| `PUT /api/reservations/{id}` | dono, RESPONSAVEL ou ADMINISTRADOR | `{startsAt, endsAt}` | `200`; `403`; `404`; `409` sobreposição; `400` |
| `DELETE /api/reservations/{id}` | dono, RESPONSAVEL ou ADMINISTRADOR | — | `204` e a reserva passa a `CANCELADA` (transição auditada, horário liberado, registro mantido); `403`; `404`; `409` se `EM_USO`/`CONCLUIDA` ou já em estado final |
| `POST /api/reservations/{id}/approval` | RESPONSAVEL | — | `200` `{status}` (`APROVADA`; permanece `SOLICITADA` se o Responsável for o próprio solicitante); `403` |
| `POST /api/reservations/{id}/rejection` | RESPONSAVEL | — | `200` `{status:"REJEITADA"}` se estava `SOLICITADA`; permanece como estava se o Responsável for o próprio solicitante ou a transição não for permitida; `403` para outros perfis; `404` |
| `POST /api/reservations/{id}/no-show` | RESPONSAVEL, ADMINISTRADOR | — | `200` `{status:"NAO_COMPARECEU"}` se estava `APROVADA`; permanece como estava se a transição não for permitida; `403` para o Solicitante; `404` |
| `POST /api/reservations/{id}/start` | dono, RESPONSAVEL ou ADMINISTRADOR | — | `200` `{status:"EM_USO"}` se estava `APROVADA`; `403`; `404` |
| `POST /api/reservations/{id}/complete` | dono, RESPONSAVEL ou ADMINISTRADOR | — | `200` `{status:"CONCLUIDA"}` se estava `EM_USO`; `403`; `404` |
| `GET /api/reservations/{id}/history` | RESPONSAVEL, ADMINISTRADOR | — | `200` lista `{from, to, actorId, changedAt, reason}` |
| `POST /api/reservations/restricted` | autenticado | — | `201` cria uma solicitação restrita avulsa (fluxo de aprovação, sem agenda) |

Transição não permitida pelo estado atual devolve `200` com o `status` inalterado, ou `409` quando o estado mudou durante a operação.

## Materiais (RF-09)

Perfis: RESPONSAVEL e ADMINISTRADOR. Só recursos do tipo `MATERIAL`; no máximo uma retirada em aberto por material.

| Método e caminho | Corpo / parâmetros | Resposta |
|---|---|---|
| `POST /api/materials/{resourceId}/withdrawals` | `{borrowerId, notes?}` | `201` `{id, resourceId, borrowerId, registeredBy, withdrawnAt, returnedAt:null, ...}`; `404` recurso; `400` não é material; `409` já retirado |
| `POST /api/loans/{loanId}/return` | — | `200` com `returnedAt` e `returnedRegisteredBy`; `404`; `409` já devolvido |
| `GET /api/loans/{loanId}` | — | `200`; `404` |
| `GET /api/loans` | `resourceId?`, `open?` (true = em aberto) | `200` lista |

## Notificações (RF-11)

| Método e caminho | Perfil | Parâmetros | Resposta |
|---|---|---|---|
| `GET /api/notifications` | autenticado | `all` (somente RESPONSAVEL/ADMINISTRADOR), `limit` (1–500, padrão 100) | `200` lista `{id, event (CRIACAO/ALTERACAO/CANCELAMENTO), reservationId, recipientId, message, delivery:"SIMULADA", createdAt}` |

## Relatório de utilização (RF-12)

Somente com o perfil `postgres`. `GET /relatorios/utilizacao?inicio=<instante>&fim=<instante>` (RESPONSAVEL, ADMINISTRADOR). Detalhes e exemplo no [README](../README.md#relatório-de-utilização). `400` quando `inicio` não precede `fim`; `401`/`403` conforme a matriz.

## Exemplo rápido

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"solicitante","password":"<senha-de-demonstracao>"}' | jq -r .access_token)
curl -s -X POST localhost:8080/api/reservations -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"resourceId":"sala-101","startsAt":"2027-03-01T10:00:00Z","endsAt":"2027-03-01T11:00:00Z"}'
```
