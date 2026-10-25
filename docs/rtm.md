# Matriz de Rastreabilidade de Requisitos (RTM)

## Objetivo e fontes

Vincular cada requisito documentado a cenários executáveis, testes de apoio e commits. As fontes funcionais e não funcionais permanecem em [requisitos-funcionais.md](requisitos-funcionais.md) e [requisitos-nao-funcionais.md](requisitos-nao-funcionais.md). A estratégia e a criticidade inicial estão em [matriz-rastreabilidade.md](matriz-rastreabilidade.md).

A feature [aprovacao-bloqueio.feature](../src/test/resources/features/aprovacao-bloqueio.feature) preserva os passos Dado/Quando/Então dos seis cenários de API do [BDD de aprovação e bloqueio](testes/bdd-aprovacao-bloqueio.md). Os steps usam MockMvc com JWT de teste e chamam a API Spring.

**Estado local:** `./mvnw -B verify` executou 18 testes (12 JUnit + 6 cenários Cucumber) e passou. JaCoCo: 93,1% de linhas e 72,5% de branches, acima dos mínimos de 80%/70%. O Quality Gate do SonarCloud ainda depende da execução no pull request.

## Requisitos Funcionais

| ID | Cenário executável / teste | Arquivo e nome | Commit associado | Estado / lacuna |
|---|---|---|---|---|
| RF-01 | API — Impedir autoaprovação; RF-07 — Solicitante não pode aprovar solicitação restrita | `aprovacao-bloqueio.feature`: `API — Impedir autoaprovação`; `ReservationApprovalTest#requesterCannotApproveOwnReservationEvenWithResponsibleRole`; `ReservationApprovalTest#requesterCannotApproveAnotherUsersRestrictedReservation` | `08e26df`, `bc5fda4`, `0ec56e9` | Cobertura parcial de autorização; outros perfis/ações não cobertos nesta feature. |
| RF-02 | — | — | — | Sem cenário de cadastro/consulta de recursos neste escopo. |
| RF-03 | — | — | — | Sem cobertura de filtros de pesquisa. |
| RF-04 | — | — | — | Criação de reserva restrita é pré-condição de cenário, mas alteração/cancelamento próprios não têm cenário dedicado. |
| RF-05 | RF-08 — Bloquear reserva durante manutenção | `aprovacao-bloqueio.feature`: `RF-08 — Bloquear reserva durante manutenção`; `MaintenanceRuleTest#reservationCrossingMaintenanceStartByOneSecondIsRejected`; `MaintenanceRuleTest#reservationOutsideMaintenancePeriodRemainsReservable` | `08e26df`, `d11309e` | Cobertos uma sobreposição positiva e período não sobreposto. Limites exatos seguem pendentes por decisão de produto. |
| RF-06 | — | — | — | Sem teste de concorrência/Testcontainers. |
| RF-07 | RF-07 — Aprovação antes de considerar aprovada; RF-07 — Decisão por Responsável; API — Impedir autoaprovação; RF-07 — Solicitante não pode aprovar solicitação restrita | `aprovacao-bloqueio.feature`: mesmos nomes; `ReservationApprovalTest#restrictedResourceReservationRemainsRequestedUntilResponsibleApproval`; `ReservationApprovalTest#responsibleApprovalRecordsDecisionAndMovesReservationToApproved`; `ReservationApprovalTest#requesterCannotApproveOwnReservationEvenWithResponsibleRole`; `ReservationApprovalTest#requesterCannotApproveAnotherUsersRestrictedReservation`; `ReservationApiTest#restrictedReservationApiReturnsSolicitadaBeforeResponsibleDecision`; `ReservationApiTest#apiRejectsRequesterApprovingOwnRestrictedReservation` | `08e26df`, `1fe463b`, `61d0ed6`, `bc5fda4`, `0ec56e9`, `f4a88d5`, `e6ac19c` | Cobertura de pendência, decisão Responsável e negações por identidade/perfil. |
| RF-08 | RF-08 — Bloquear reserva durante manutenção | `aprovacao-bloqueio.feature`: `RF-08 — Bloquear reserva durante manutenção`; `ReservationApiTest#apiRejectsReservationCrossingMaintenanceAndDoesNotStoreIt`; `MaintenanceRuleTest#reservationCrossingMaintenanceStartByOneSecondIsRejected` | `08e26df`, `d11309e`, `2042544` | Sobreposição por um segundo é recusada e não armazenada; consulta reporta indisponibilidade. Contatos exatos não foram decididos nem testados. |
| RF-09 | — | — | — | Sem cenário de retirada/devolução. |
| RF-10 | RF-07 — Decisão por Responsável; API — Fluxo principal e auditoria; RF-14 — Reserva concluída não aceita nova aprovação | `aprovacao-bloqueio.feature`: cenários citados; `ReservationLifecycleTest#reservationFollowsMainLifecycleAndAuditsEveryTransition`; `ReservationLifecycleTest#completedReservationPreservesApprovalDecisionWhenApprovalIsRepeated`; `ReservationApiTest#apiRunsMainLifecycleAndReturnsThreeAuditTransitions` | `08e26df`, `7b6c906`, `bea1e5b`, `3e7d061` | Auditoria de transições do fluxo principal tem ator e instante; auditoria geral de todos os registros não está coberta. |
| RF-11 | — | — | — | Sem cenário de notificações. |
| RF-12 | — | — | — | Sem cenário de relatórios. |
| RF-13 | — | — | — | Sem teste parametrizado de término anterior/igual/posterior ao início nesta feature. |
| RF-14 | RF-14 — Fluxo principal permitido; API — Fluxo principal e auditoria; RF-14 — Reserva concluída não aceita nova aprovação | `aprovacao-bloqueio.feature`: cenários citados; `ReservationLifecycleTest#reservationFollowsMainLifecycleAndAuditsEveryTransition`; `ReservationLifecycleTest#completedReservationPreservesApprovalDecisionWhenApprovalIsRepeated`; `ReservationApiTest#apiRunsMainLifecycleAndReturnsThreeAuditTransitions` | `08e26df`, `7b6c906`, `bea1e5b`, `3e7d061` | Fluxo principal coberto. O salto `SOLICITADA → EM_USO` não tem teste dedicado; alternativas aguardam matriz de transições aprovada. |
| RF-15 | — | — | — | Sem cenário de remoção negada em `EM_USO`. |

## Requisitos Não Funcionais

| ID | Cenário executável / teste | Arquivo e nome | Commit associado | Estado / lacuna |
|---|---|---|---|---|
| RNF-01 | API — Impedir autoaprovação; RF-07 — Solicitante não pode aprovar solicitação restrita; cenário de administração da manutenção | `aprovacao-bloqueio.feature`: `API — Impedir autoaprovação`, `RF-07 — Solicitante não pode aprovar solicitação restrita`, `RF-08 — Bloquear reserva durante manutenção` | `08e26df`, `e6ac19c` | Tokens JWT de teste validam perfil/identidade no MockMvc; autenticação criptográfica de produção não é validada por estes testes. |
| RNF-02 | — | — | — | Sem execução concorrente repetível ou Testcontainers. |
| RNF-03 | RF-07 — Decisão por Responsável; API — Fluxo principal e auditoria | `aprovacao-bloqueio.feature`: cenários citados; `ReservationLifecycleTest#reservationFollowsMainLifecycleAndAuditsEveryTransition`; `ReservationApiTest#apiRunsMainLifecycleAndReturnsThreeAuditTransitions` | `08e26df`, `7b6c906`, `3e7d061` | Ator e instante verificados para mudanças do fluxo principal; retenção/imutabilidade pendentes. |
| RNF-04 | — | — | — | Sem E2E de interface/responsividade. |
| RNF-05 | — | — | — | Esta RTM não substitui documentação de contrato da API. |
| RNF-06 | — | — | — | Sem plano ou resultado JMeter. |
| RNF-07 | As seis features da funcionalidade; suíte JUnit 5; execução Maven/CI | `aprovacao-bloqueio.feature`; `RunCucumberTest`; `ReservationApiTest`; `MaintenanceRuleTest`; `.github/workflows/ci.yml` | `08e26df` | `./mvnw -B verify` local passou; JaCoCo 93,1% linhas/72,5% branches. O Quality Gate SonarCloud será registrado após execução no PR. |

## Execução e gates

- Maven executa Cucumber pela suíte JUnit Platform `RunCucumberTest` durante `test`, incluída em `./mvnw -B verify`.
- JaCoCo mantém os limites configurados no POM: 80% de linhas e 70% de branches; nenhuma meta foi reduzida e nenhuma exclusão nova foi aplicada.
- GitHub Actions executa `./mvnw -B verify` e o scanner SonarCloud em pull requests, aguardando o Quality Gate.

## Lacunas que permanecem abertas

- Resolver os dois contatos exatos do período de manutenção antes de adicionar expectativas Gherkin.
- Definir matriz de origem, destino e perfil para `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`.
- Adicionar teste próprio para o salto `SOLICITADA → EM_USO`.
- Cobrir concorrência, cadastro/pesquisa, retirada/devolução, notificações, relatórios, validade do intervalo, exclusão em `EM_USO`, UI responsiva e desempenho conforme os respectivos requisitos.
