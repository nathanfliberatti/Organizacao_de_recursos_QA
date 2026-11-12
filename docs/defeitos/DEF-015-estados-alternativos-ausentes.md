# DEF-015: Estados alternativos do ciclo de vida não implementados

- **Severidade / prioridade:** Alta / P1 — a especificação lista "estados alternativos: rejeitada, cancelada e não compareceu" entre as **regras críticas** (RF-14), e a meta "100% dos requisitos críticos na RTM" depende disso.
- **Situação:** Corrigido em `e264520` (commit da branch da entrega; na `main` fica dentro do squash do PR)
- **Encontrado em:** 2026-11-12, ao comparar a entrega com a especificação oficial do professor
- **Evidência:** `ReservationStatus` tinha só `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`. O requisito vinha tratado como "risco aceito R1" por falta de uma matriz de transições, mas a especificação pede a **representação** de cada estado (RF-14).
- **Risco:** requisito crítico parcialmente atendido; cancelar era só uma exclusão lógica **sem** registro de auditoria da mudança de estado, o que contraria "toda mudança de estado deve gerar auditoria".
- **Correção:** estados `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` com matriz de transições única no enum (`canMoveTo`), estados finais, auditoria de cada transição, horário devolvido à agenda, endpoints `POST /api/reservations/{id}/rejection` e `/no-show`, `DELETE` passa a registrar a transição para `CANCELADA`, constraints do banco recriadas (migração idempotente) e interface com os botões novos.
- **Teste de regressão:** `ReservationStatusTransitionTest` (49 pares), `ReservationAlternativeStatesTest`, `InMemoryReservationAuditServiceTest`, três fluxos em `AbstractApplicationFlowTest`, duas linhas novas em `AuthorizationMatrixIT` e nove verificações novas no E2E.
- **Decisão de negócio adotada (não validada com usuários):** rejeitar = só Responsável, de `SOLICITADA`; cancelar = dono, Responsável ou Administrador, de `SOLICITADA` ou `APROVADA`; não compareceu = Responsável ou Administrador, de `APROVADA`. Não há regra de tempo para registrar o não comparecimento.
