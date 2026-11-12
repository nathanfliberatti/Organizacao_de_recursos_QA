# DEF-014: Modo em memória permitia dupla reserva

- **Severidade / prioridade:** Alta / P1 — "possibilidade de dupla reserva" é limitador de nota na especificação e o README oferecia esse modo.
- **Situação:** Corrigido em `003867a` (commit da branch da entrega; na `main` fica dentro do squash do PR)
- **Encontrado em:** 2026-11-12, ao comparar a entrega com a especificação oficial do professor
- **Evidência:** `InMemoryReservationStore.isAvailable` devolvia sempre `true` e `scheduleWithId` aceitava qualquer reserva; o teste `InMemoryReservationStoreTest#acceptsReservationsAndReportsAvailability` **afirmava** esse comportamento (depois de reservar, o horário seguia "disponível").
- **Risco:** quem seguisse a opção "sem banco" do README podia reservar a mesma sala ou o mesmo professor duas vezes no mesmo horário, inclusive com solicitações simultâneas.
- **Correção:** o store em memória passa a recusar sobreposição de sala e de professor (intervalos semiabertos `[início, fim)`) de forma atômica, na criação e na alteração; reservas canceladas liberam o horário. O teste anterior foi corrigido e há testes novos.
- **Teste de regressão:** `InMemoryReservationStoreTest` (parametrizado por tipo de sobreposição, professor, cancelamento e 16 requisições simultâneas); `AbstractApplicationFlowTest#overlappingReservationForSameResourceIsRejectedWithConflict` e `#concurrentRequestsForSameSlotConfirmOnlyOneReservation`, que agora rodam em memória **e** em PostgreSQL.
- **Limite que permanece:** a garantia entre várias instâncias da aplicação continua sendo do PostgreSQL (ADR-001); o modo em memória vale para uma única instância.
