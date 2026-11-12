# DEF-004: Endpoint de alteração de reserva ausente

- **Severidade / prioridade:** Média / P2 — Funcionalidade obrigatória (alterar reserva) ausente; sem risco de integridade.
- **Situação:** Corrigido em `91fb390` e endurecido para reservas legadas em `c205d77`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix` e `AuthorizationMatrixIT.requesterCannotModifyAnotherRequestersReservation`
- **Operação:** `PUT /api/reservations/{id}`
- **Células divergentes:** Solicitante, Responsável e Administrador esperam `200`, recebem `405`; Solicitante alheio espera `403`, recebe `405`.
- **Risco:** Não há forma de alterar a reserva nem de aplicar a regra de posse solicitada.
- **Correção esperada:** Implementar atualização de `startsAt`/`endsAt`, persistir o proprietário da reserva e negar `403` quando Solicitante tentar alterar reserva alheia.
