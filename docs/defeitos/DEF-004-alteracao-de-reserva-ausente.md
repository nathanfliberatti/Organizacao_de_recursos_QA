# DEF-004: Endpoint de alteração de reserva ausente

- **Situação:** Aberto
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix` e `AuthorizationMatrixIT.requesterCannotModifyAnotherRequestersReservation`
- **Operação:** `PUT /api/reservations/{id}`
- **Células divergentes:** Solicitante, Responsável e Administrador esperam `200`, recebem `405`; Solicitante alheio espera `403`, recebe `405`.
- **Risco:** Não há forma de alterar a reserva nem de aplicar a regra de posse solicitada.
- **Correção esperada:** Implementar atualização de `startsAt`/`endsAt`, persistir o proprietário da reserva e negar `403` quando Solicitante tentar alterar reserva alheia.
