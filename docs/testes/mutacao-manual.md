# Mutação manual dos requisitos críticos

Rodada refeita no commit atual da entrega. Substitui a rodada anterior (commit `fa13d6c`, 137 testes, "CI 0/10"), que ficou obsoleta.

## 1. Identificação

| Item | Valor |
|---|---|
| Código de produção avaliado | `src/main` idêntico ao da `main` em `3e849fa` (merge do PR #13). Nenhuma linha de produção foi alterada nesta rodada, só testes. |
| Commit onde as mutações foram aplicadas | `736598a` (branch `entrega/matriz-mutacao-relatorio`) |
| Commit com os testes que fecharam as lacunas | `3f2ed8f` (CI no commit `30bae72`, que inclui os documentos) |
| Data | 2026-11-12 |
| Ambiente | Contêiner Linux, Java 21, Maven Wrapper, PostgreSQL 16 local (sem Docker) |
| Executor | Claude Code, a pedido de Nathan Ferraz Liberatti |

## 2. Método e limite

Mutação **manual**, não estatística: para cada regra crítica, introduzi uma quebra pequena e plausível no código de produção, executei os testes que deveriam proteger a regra e voltei o arquivo com `git checkout` antes da mutação seguinte. O script que executa o ciclo (aplicar, testar, restaurar) fica fora do repositório; cada linha da tabela abaixo é reproduzível com a quebra e o comando descritos.

- **Mutantes de regra de negócio** rodaram a suíte unitária completa (`./mvnw test`, 181–182 testes).
- **Mutantes de PostgreSQL** (SQL, restrição de exclusão, transações) rodaram cópias dos ITs apontadas para o PostgreSQL 16 local, porque o sandbox não tem Docker. Os ITs originais, com Testcontainers, rodam no CI. As cópias são idênticas, salvo a origem do banco, e passaram 100% sem mutação antes de cada bateria (158 testes no baseline: `ReservationConcurrencyIT` 11, `ReservationAuditIT` 7, `AuthorizationMatrixFullIT` 140); depois dos testes novos, `ReservationAuditIT` tem 9 e `ApplicationFlowIT` 15.
- **Limite:** 21 mutantes escolhidos por mim. Não substituem uma ferramenta como PIT, não cobrem todas as linhas e o escore abaixo vale somente para este conjunto.

## 3. Resultado por mutante

"Morto" = pelo menos um teste falhou com a mutação. "Sobreviveu" = nenhum teste falhou.

| ID | Requisito | Quebra aplicada | Onde | Resultado | Teste que detectou |
|---|---|---|---|---|---|
| M01 | RF-05 | Operador de sobreposição `&&` virou `<<` nas consultas de disponibilidade | `PostgresReservationStore` | Morto | `ReservationConcurrencyIT.roomAvailabilityQueriesPostgresWithoutProfessorFilter` |
| M02 | RF-05 | `second.startsAt().isBefore(first.endsAt())` virou `isAfter` | `InMemoryReservationStore.overlaps` | Morto (15 testes) | `ApplicationFlowTest` (concorrência no mesmo horário, manutenção, cancelamento) |
| M03a | RF-06, RNF-02 | Removi o `pg_advisory_xact_lock` | `PostgresReservationStore` | **Sobreviveu** | Nenhum. A restrição `EXCLUDE` do banco e a consulta de disponibilidade continuam impedindo a dupla reserva. |
| M03b | RF-06, RNF-02 | Restrições `EXCLUDE` desligadas (`WHERE (false)`) | `schema.sql` | **Sobreviveu** | Nenhum. A trava de aviso e a consulta de disponibilidade continuam impedindo a dupla reserva. |
| M03c | RF-06, RNF-02 | M03a + M03b juntos | `PostgresReservationStore` + `schema.sql` | Morto (8 testes) | `ReservationConcurrencyIT.onlyOneConcurrentReservationIsAcceptedForSameRoomAndTime` e `...SameProfessorAndTime` |
| M04 | RF-07 | `isEligibleResponsible` passou a retornar `true` | `Reservation` | Morto (7 testes) | `ReservationApprovalTest.requesterCannotApproveAnotherUsersRestrictedReservation`, `ReservationApiTest.apiRejectsRequesterApprovingOwnRestrictedReservation` |
| M05 | RF-08 | `MaintenanceRule.isReservable` passou a retornar `true` | `MaintenanceRule` | Morto (4 testes) | `MaintenanceRuleTest.reservationCrossingMaintenanceStartByOneSecondIsRejected`, `ReservationApiTest.apiRejectsReservationCrossingMaintenanceAndDoesNotStoreIt` |
| M06 | RF-08 | Borda final: `isAfter(startsAt)` virou `!isBefore(startsAt)` (reserva colada no início da manutenção passa a ser bloqueada) | `MaintenancePeriod.overlaps` | **Sobreviveu na primeira rodada**; morto depois do teste novo | `MaintenanceRuleTest.reservationsTouchingTheMaintenanceBoundariesAreReservable` |
| M07 | RF-08 | Borda inicial: `isBefore(endsAt)` virou `!isAfter(endsAt)` | `MaintenancePeriod.overlaps` | Morto (executado só depois do teste novo; sem resultado "antes") | idem M06 |
| M08 | RF-13 | Aceita duração zero: `!startsAt.isBefore(endsAt)` virou `endsAt.isBefore(startsAt)` | `ReservationSchedulingService.validatePeriod` | Morto | `ReservationIntervalValidationTest.rejectsEqualOrInvertedIntervalAndAcceptsValidOne` |
| M09 | RF-14 | `SOLICITADA` passou a poder ir direto para `EM_USO` | `ReservationStatus.canMoveTo` | Morto (3 testes) | `ReservationStatusTransitionTest.onlyTheDocumentedTransitionsAreAllowed`, `ReservationLifecycleTest.reservationCannotStartUseBeforeApproval` |
| M10 | RF-10, RNF-03 | Removi o `insertAudit` | `ReservationAuditService` | Morto (3 testes) | `ReservationAuditIT.stateAndAuditArePersistedTogether` (esperado 2 registros, veio 1) |
| M11 | RF-10, RNF-03 | Removi o `@Transactional` do serviço de auditoria | `ReservationAuditService` | **Sobreviveu na primeira rodada**; morto depois do teste novo | `ReservationAuditIT.failedAuditInsertRollsBackTheStateChangeWithoutAnOuterTransaction` (estado ficou `APROVADA` sem registro de auditoria) |
| M12 | RF-14 | Removi `APROVADA → NAO_COMPARECEU` | `ReservationStatus.canMoveTo` | Morto (5 testes) | `ReservationStatusTransitionTest.onlyTheDocumentedTransitionsAreAllowed`, `ReservationAlternativeStatesTest.responsibleAndAdministratorRegisterNoShowOfAnApprovedReservation` |
| M13 | RF-15 | `started()` passou a retornar `false` | `ReservationStatus` | Morto (3 testes) | `ReservationStatusTransitionTest.startedAndReleasingFlagsMatchTheRules` |
| M14 | RF-15 | Removi o bloqueio de exclusão de reserva `EM_USO`/`CONCLUIDA` | `ReservationAuditService.deleteReservation` | Morto (2 testes) | `ReservationAuditIT.startedReservationCannotBeDeletedAndItsHistoryRemains`, `completedReservationCannotBeDeleted` |
| M15 | RF-15 | Removi o bloqueio `started()` no cancelamento auditado | `ReservationAuditService.cancelReservation` | **Sobreviveu na primeira rodada**; morto depois do teste novo | `ReservationAuditIT.startedReservationCannotBeCancelledThroughTheAuditedTransition` |
| M16 | Autorização | Removi o `@PreAuthorize` do histórico | `ReservationController` | Morto | `AuthorizationMatrixFullIT`: `GET /api/reservations/{id}/history` como Solicitante |
| M17 | Autorização | Aprovação aberta a qualquer autenticado | `ReservationController` | Morto (2 testes) | `AuthorizationMatrixFullIT`: `POST .../approval` como Solicitante e como Administrador |
| M18 | Autorização | Removi a checagem de dono na alteração | `ReservationSchedulingService` | Morto | `AuthorizationMatrixFullIT`: `PUT /api/reservations/{id}` reserva alheia, Solicitante |
| M19 | Autorização | Removi a checagem de dono no cancelamento | `ReservationAuditService.cancelReservation` | Morto | `AuthorizationMatrixFullIT`: `DELETE /api/reservations/{id}` reserva alheia, Solicitante |

### Sobreviventes e como foram tratados

- **M06, M11, M15 revelaram lacunas reais** e foram corrigidas com testes novos (commit `3f2ed8f`):
  - M06: nenhum teste cobria a reserva que termina exatamente quando a manutenção começa.
  - M11: o teste de rollback já existente roda dentro de uma transação externa, então não provava que o serviço sozinho é atômico. Sem `@Transactional`, uma falha no INSERT da auditoria deixava o estado alterado sem registro. O teste novo força a falha com uma restrição temporária no banco e confirma que o estado volta a `SOLICITADA`.
  - M15: o cancelamento de reserva `EM_USO` já devolvia 409 pela regra de transição, então o status HTTP não distingue o mutante. O corpo do erro não expõe a mensagem de propósito. O teste novo confere o tipo da exceção no serviço e o estado preservado.
  - Para cada um, confirmei que o teste novo **passa no código original** e **falha no mutante**.
- **M03a e M03b são sobreviventes por redundância**, não por falta de teste: a trava de aviso, a consulta de disponibilidade e a restrição `EXCLUDE` protegem a mesma regra (ADR-001, defesa em profundidade). Cada uma sozinha não causa dupla reserva; as duas juntas (M03c) causam e foram detectadas. Foram mantidos na contagem como sobreviventes.

## 4. Escore

| Medida | Valor | Observação |
|---|---|---|
| Mutantes executados | 21 | |
| Mortos na primeira rodada | 15 de 20 avaliados | M07 só foi executado depois do teste novo e não entra nesta conta |
| Sobreviventes na primeira rodada | 5 (M03a, M03b, M06, M11, M15) | 3 por lacuna real, 2 por redundância |
| Mortos depois dos testes novos | 19 de 21 (90,5%) | Restam M03a e M03b, redundantes isoladamente |
| Mortos, excluindo os dois redundantes | 19 de 19 (100%) | |

## 5. Requisitos críticos com evidência

Critérios, todos exigidos: (a) risco identificado em [riscos.md](../riscos.md) e na RTM; (b) teste localizável por arquivo e nome; (c) teste executado com sucesso no commit avaliado; (d) pelo menos um mutante da regra detectado pelo teste; (e) CI verde no commit final.

| Requisito | (a) Risco | (b) Teste localizável | (c) Executado | (d) Mutantes detectados | (e) CI no commit final |
|---|---|---|---|---|---|
| RF-05 Sobreposição | Sim | Sim | Sim | M01, M02 | Sim, PR #14 |
| RF-06 Dupla reserva | Sim | Sim | Sim | M03c | Sim, PR #14 |
| RF-07 Aprovação de restrito | Sim | Sim | Sim | M04 | Sim, PR #14 |
| RF-08 Manutenção | Sim | Sim | Sim | M05, M06, M07 | Sim, PR #14 |
| RF-10 Auditoria | Sim | Sim | Sim | M10, M11 | Sim, PR #14 |
| RF-13 Intervalo | Sim | Sim | Sim | M08 | Sim, PR #14 |
| RF-14 Ciclo de vida | Sim | Sim | Sim | M09, M12 | Sim, PR #14 |
| RF-15 Preservação após início | Sim | Sim | Sim | M13, M14, M15 | Sim, PR #14 |
| RNF-02 Integridade concorrente | Sim | Sim | Sim | M03c | Sim, PR #14 |
| RNF-03 Rastreabilidade | Sim | Sim | Sim | M10, M11 | Sim, PR #14 |

Resultado local, sem contar o CI: **10 de 10 requisitos críticos (100%)** atendem de (a) a (d). Autorização, que a RTM cobre pela matriz, teve 4 mutantes mortos (M16 a M19) e a matriz completa (35 linhas × 4 perfis = 140 verificações) passou.

Limite: "10 de 10" significa que cada requisito tem ao menos um teste que detecta uma quebra plausível da regra. Não significa que todo comportamento do requisito está coberto, nem que não existam mutantes que eu não tenha pensado.

## 6. CI

O CI do PR #14 ([execução 43144281216](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/43144281216), commit `30bae72`) passou: Maven verify (Failsafe com 259 testes, 0 falhas), SonarCloud e Quality Gate. Isso comprova o critério (e) para o código desta rodada. O commit que receberá a tag é o da `main` depois do merge; o CI dele precisa ser conferido de novo e **ainda não existe**.

## 7. Estado final dos testes

Com as lacunas fechadas, `./mvnw -B test` no commit `3f2ed8f`: **183 testes, 0 falhas, 0 erros**. Os ITs (`*IT`) só rodam no CI; localmente rodei as cópias contra o PostgreSQL 16 (ver §2).
