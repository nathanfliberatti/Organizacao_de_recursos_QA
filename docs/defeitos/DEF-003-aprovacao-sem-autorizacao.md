# DEF-003: Aprovação responde sucesso a perfis não autorizados

- **Severidade / prioridade:** Alta / P1 — Perfil sem permissão aprovava reserva: falha de autorização (limitador de nota).
- **Situação:** Corrigido em `85a1106`; regressão Cucumber em `7ea7894`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`
- **Operação:** `POST /api/reservations/{id}/approval`
- **Células divergentes:** Solicitante e Administrador esperam `403`; ambos recebem `200`.
- **Risco:** A resposta de sucesso mascara uma tentativa de aprovação proibida e não comunica corretamente a política exclusiva do Responsável.
- **Correção esperada:** Restringir a operação ao papel Responsável e devolver `403` aos demais perfis autenticados.
