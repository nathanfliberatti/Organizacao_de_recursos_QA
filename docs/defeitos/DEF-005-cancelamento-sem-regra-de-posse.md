# DEF-005: Solicitante pode cancelar reserva alheia

- **Severidade / prioridade:** Alta / P1 — Cancelamento sem verificar posse: usuário agia sobre reserva alheia.
- **Situação:** Corrigido em `7d230a4`
- **Evidência:** `AuthorizationMatrixIT.requesterCannotCancelAnotherRequestersReservation`
- **Operação:** `DELETE /api/reservations/{id}`
- **Célula divergente:** Solicitante que não é proprietário espera `403`; recebe `204`.
- **Risco:** Um usuário autenticado pode cancelar a reserva de outra pessoa.
- **Correção esperada:** Verificar a identidade do proprietário para Solicitante antes de alterar o estado; Responsável e Administrador mantêm a política definida na matriz.
