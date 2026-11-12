# Arquitetura do sistema implementado

Este documento descreve o que **foi entregue** em `v1.0.0-rc1`. O [documento de arquitetura](arquitetura.md) é o desenho original (anterior à implementação) e inclui propostas que não foram construídas, como o job diário de treinamento. Em caso de divergência, vale este arquivo; os nomes abaixo são os das classes em `src/main/java/br/com/organizacao/recursos/reservas`.

> Os diagramas estão em Mermaid (renderizam no GitHub). Cópias em imagem: [docs/diagramas/](diagramas/).

## 1. Contexto (C4 nível 1)

```mermaid
C4Context
  title Organização de Recursos — contexto
  Person(solicitante, "Solicitante", "Reserva salas e materiais; altera e cancela as próprias reservas")
  Person(responsavel, "Responsável", "Aprova reservas de recursos restritos; registra retirada/devolução; vê relatórios")
  Person(admin, "Administrador", "Cadastra recursos e usuários; bloqueia recursos em manutenção")
  System(sistema, "Sistema de Organização de Recursos", "Aloca salas, professores e materiais evitando conflitos de horário")
  System_Ext(notificacao, "Canal de notificação", "Simulado: o evento é registrado no banco; sem integração externa")
  Rel(solicitante, sistema, "Usa", "HTTPS, navegador")
  Rel(responsavel, sistema, "Usa", "HTTPS, navegador")
  Rel(admin, sistema, "Usa", "HTTPS, navegador")
  Rel(sistema, notificacao, "Registra criação, alteração e cancelamento", "simulado")
```

## 2. Contêineres (C4 nível 2)

```mermaid
flowchart LR
  subgraph Navegador
    SPA["SPA estática<br/>HTML + CSS + JS (módulos ES)<br/>src/main/resources/static"]
  end
  subgraph JVM["Aplicação Spring Boot 3.4 / Java 21"]
    API["API REST stateless<br/>(controllers + segurança JWT)"]
    STATIC["Servidor de arquivos estáticos"]
  end
  DB[("PostgreSQL 16<br/>schema.sql<br/>EXCLUDE gist + advisory locks")]
  SPA -- "GET / (arquivos)" --> STATIC
  SPA -- "JSON + Authorization: Bearer" --> API
  API -- "JDBC (perfil postgres)" --> DB
```

Dois modos de execução, escolhidos por perfil do Spring:

| Perfil | Armazenamento | Uso |
|---|---|---|
| `postgres` | PostgreSQL (`Postgres*Store`, `PostgresMaintenanceCatalog`) | Suportado; garante integridade sob concorrência (ADR-001) |
| (padrão) | Memória (`InMemory*Store`) | Somente demonstração; **não detecta sobreposição de horários** |
| `perf` | Login de carga (`PerformanceAuthController`), usado com `postgres` | Teste de desempenho local |

## 3. Componentes da aplicação (C4 nível 3)

```mermaid
flowchart TB
  subgraph Seguranca["Segurança"]
    SEC["AppSecurityConfiguration<br/>SecurityFilterChain: JWT HS256, claim roles, stateless<br/>@EnableMethodSecurity (ReservationSecurityConfiguration)"]
  end
  subgraph Controllers
    AUTH[AuthController]
    USER[UserController]
    RES[ResourceController]
    SEARCH[ResourceSearchController]
    MAINT[MaintenanceController]
    RESV[ReservationController]
    QUERY[ReservationQueryController]
    LOAN[MaterialLoanController]
    NOTIF[NotificationController]
    REPORT["UtilizationReportController<br/>(só perfil postgres)"]
  end
  subgraph Servicos["Regras de negócio"]
    SCHED[ReservationSchedulingService]
    EXTRAS["ReservationExtras<br/>liga agenda ao ciclo de vida e notificações"]
    AUDIT["ReservationAuditService<br/>estado + trilha de auditoria transacional"]
    CAT["ReservationCatalog<br/>estado em memória do ciclo (reconstruído da auditoria)"]
    NS[NotificationService]
    MC[MaintenanceCatalog]
    RULE[MaintenanceRule]
    RSVC[UtilizationReportService]
  end
  subgraph Stores["Persistência (uma implementação por perfil)"]
    RS[ReservationStore]
    RES_S[ResourceStore]
    US[UserStore]
    LS[MaterialLoanStore]
    NS_S[NotificationStore]
  end
  DB[("PostgreSQL")]
  SEC --> Controllers
  AUTH --> US
  USER --> US
  RES --> RES_S
  SEARCH --> RES_S
  SEARCH --> SCHED
  MAINT --> MC
  RESV --> SCHED
  RESV --> AUDIT
  RESV --> CAT
  RESV --> EXTRAS
  QUERY --> EXTRAS
  LOAN --> LS
  LOAN --> RES_S
  NOTIF --> NS_S
  REPORT --> RSVC
  SCHED --> RS
  SCHED --> MC
  SCHED --> RULE
  EXTRAS --> AUDIT
  EXTRAS --> CAT
  EXTRAS --> NS
  EXTRAS --> RES_S
  NS --> NS_S
  RS --> DB
  RES_S --> DB
  US --> DB
  LS --> DB
  NS_S --> DB
  AUDIT --> DB
  MC --> DB
  RSVC --> DB
```

Pontos de projeto relevantes:

- **Autorização na fronteira do servidor:** `@PreAuthorize` em cada operação protegida; a interface só esconde ações (RF-01, RNF-01). Matriz verificada por `AuthorizationMatrixIT`.
- **Dois registros por reserva:** a **agenda** (`reservations`, com as constraints de exclusão) e o **ciclo de vida auditado** (`reservation_lifecycle` + `reservation_audit`), ligados pelo id (`reservation_lifecycle.reservation_id = reservations.id::text`). `ReservationExtras` cria o segundo quando o primeiro é aceito.
- **Integridade de horário:** decidida pelo banco (ADR-001), não pela aplicação.

## 4. Sequências críticas

### 4.1 Login

```mermaid
sequenceDiagram
  actor U as Usuário
  participant SPA as SPA
  participant A as AuthController
  participant S as UserStore
  U->>SPA: usuário e senha
  SPA->>A: POST /api/auth/login
  A->>S: findByUsername
  A->>A: BCrypt.matches (compara um hash mesmo se o usuário não existir)
  alt credenciais válidas e usuário ativo
    A-->>SPA: 200 { access_token (JWT 8 h), user { role } }
    SPA->>SPA: guarda o token em sessionStorage
  else inválidas
    A-->>SPA: 401
  end
```

### 4.2 Criar reserva (recurso livre ou restrito, conflito e manutenção)

```mermaid
sequenceDiagram
  actor U as Solicitante
  participant C as ReservationController
  participant SC as ReservationSchedulingService
  participant M as MaintenanceCatalog
  participant ST as ReservationStore (PostgreSQL)
  participant X as ReservationExtras
  participant AU as ReservationAuditService
  participant N as NotificationService
  U->>C: POST /api/reservations {resourceId, startsAt, endsAt, professorId?}
  C->>SC: schedule(request, requester)
  SC->>SC: valida término > início (senão 400)
  SC->>M: período de manutenção do recurso?
  alt cruza a manutenção
    SC-->>C: accepted=false
    C-->>U: 409
  else livre de manutenção
    SC->>ST: scheduleWithId (locks + INSERT, ver 4.4)
    alt sobreposição de sala ou professor
      SC->>ST: recordConflictAttempt
      SC-->>C: accepted=false
      C-->>U: 409
    else aceita
      SC-->>C: accepted=true, reservationId
      C->>X: onCreated
      X->>AU: createReservation → SOLICITADA (auditado)
      alt recurso restrito
        Note over X,AU: permanece SOLICITADA até um Responsável aprovar (4.3)
      else recurso não restrito
        X->>AU: changeState → APROVADA (ator "sistema")
      end
      X->>N: notify CRIACAO (simulada)
      C-->>U: 201 { accepted: true, reservationId }
    end
  end
```

### 4.3 Aprovação, uso e conclusão (RF-07, RF-14, RF-15)

```mermaid
sequenceDiagram
  actor R as Responsável
  actor S as Solicitante
  participant C as ReservationController
  participant AU as ReservationAuditService
  participant DB as PostgreSQL
  R->>C: POST /api/reservations/{id}/approval
  C->>C: @PreAuthorize RESPONSAVEL (outros perfis: 403)
  C->>C: Reservation.prepareApproval (recusa autoaprovação: o solicitante não pode ser o aprovador)
  C->>AU: recordTransition SOLICITADA → APROVADA
  AU->>DB: UPDATE lifecycle (status esperado) + INSERT audit na mesma transação
  C-->>R: 200 { status: APROVADA }
  S->>C: POST /api/reservations/{id}/start
  C->>AU: assertCanOperate (dono, Responsável ou Administrador)
  C->>AU: recordTransition APROVADA → EM_USO
  S->>C: DELETE /api/reservations/{id}
  C->>AU: deleteReservation
  AU-->>C: ReservationDeletionConflictException (EM_USO ou CONCLUIDA)
  C-->>S: 409 (o registro permanece consultável)
  S->>C: POST /api/reservations/{id}/complete
  C->>AU: recordTransition EM_USO → CONCLUIDA
```

### 4.4 Disputa concorrente pelo mesmo horário (RF-05, RF-06, ADR-001)

```mermaid
sequenceDiagram
  participant T1 as Requisição 1
  participant T2 as Requisição 2
  participant ST as PostgresReservationStore
  participant DB as PostgreSQL
  par requisição 1 (mesma sala/professor e horário)
    T1->>ST: scheduleWithId
  and requisição 2 (mesma sala/professor e horário)
    T2->>ST: scheduleWithId
  end
  ST->>DB: pg_advisory_xact_lock("professor:…") e ("room:…") em ordem lexical
  Note over ST,DB: a ordem fixa evita deadlock, a requisição 2 espera o commit da 1
  ST->>DB: SELECT NOT EXISTS (reserva ativa sobreposta)
  T1->>DB: INSERT reservations (aceita)
  T2->>DB: SELECT NOT EXISTS → falso
  T2->>ST: sem vaga (OptionalLong vazio)
  Note over DB: as constraints EXCLUDE USING gist continuam como garantia final, 23P01 e 40P01 também viram "sem vaga"
  ST-->>T1: id da reserva → 201
  ST-->>T2: conflito registrado em reservation_conflict_attempts → 409
```

### 4.5 Retirada e devolução de material (RF-09)

```mermaid
sequenceDiagram
  actor R as Responsável/Administrador
  participant L as MaterialLoanController
  participant RS as ResourceStore
  participant LS as MaterialLoanStore
  R->>L: POST /api/materials/{id}/withdrawals {borrowerId}
  L->>RS: findById (404 se não existe, 400 se não for MATERIAL)
  L->>LS: withdraw
  Note over LS: índice único parcial: no máximo uma retirada em aberto por material (409)
  L-->>R: 201 { id, withdrawnAt }
  R->>L: POST /api/loans/{id}/return
  L->>LS: giveBack (404 inexistente, 409 já devolvido)
  L-->>R: 200 { returnedAt }
```

## 5. Rastreabilidade com o código

| Diagrama | Teste que exercita |
|---|---|
| 4.1 | `ApplicationFlowTest#loginRejectsWrongPasswordAndProtectedEndpointsRequireToken`, `#roleIsReturnedByLoginAndMe` |
| 4.2 | `ApplicationFlowTest#reservationLifecycleFollowsResourceRestrictionAndNotifiesEvents`, `#maintenanceBlocksReservationsAndCanBeRemoved`, `ApplicationFlowIT#overlappingReservationForSameResourceIsRejectedWithConflict` |
| 4.3 | `ReservationLifecycleTest`, `ReservationApprovalTest`, `ReservationAuditIT`, cenários Cucumber de aprovação |
| 4.4 | `ReservationConcurrencyIT`, `ApplicationFlowIT#concurrentRequestsForSameSlotConfirmOnlyOneReservation` e a [rodada 02 de carga](performance/resultado-02.md) |
| 4.5 | `ApplicationFlowTest#materialWithdrawalAndReturnAreRecordedAndQueryable` |
