# DEF-009: Solicitante pode iniciar ou concluir reserva alheia

- **Situação:** Aberto
- **Evidência:** `AuthorizationMatrixIT.requesterCannotStartAnotherRequestersReservation` e `AuthorizationMatrixIT.requesterCannotCompleteAnotherRequestersReservation`
- **Operações:** `POST /api/reservations/{id}/start` e `POST /api/reservations/{id}/complete`.
- **Células divergentes:** Solicitante alheio espera `403` nas duas operações; ambas respondem `200`.
- **Risco:** Um Solicitante pode avançar o ciclo de vida de uma reserva de outra pessoa.
- **Correção esperada:** Verificar posse antes de aplicar ou persistir a transição; Responsável e Administrador mantêm acesso conforme a matriz.
