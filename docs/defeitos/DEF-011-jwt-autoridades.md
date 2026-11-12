# DEF-011: Autoridades dos perfis não são extraídas do JWT

- **Severidade / prioridade:** Alta / P1 — Perfis não eram extraídos do JWT: operações autorizadas eram negadas e a matriz não podia ser verificada.
- **Renumerado:** registrado originalmente como `DEF-002` (commit `5639986` e mensagens de commit da época citam `DEF-002`); renumerado para `DEF-011` por colisão com [DEF-002](DEF-002-deadlock-constraints-exclusao.md).
- **Situação:** Corrigido em `5639986`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`
- **Causa observada:** JWTs válidos e assinados com a claim `roles` autenticam, mas não produzem as autoridades `ROLE_*` esperadas por `@PreAuthorize`.
- **Células divergentes:**
  - `GET /api/reservations/{id}/history`: Responsável e Administrador esperam `200`, recebem `403`.
  - `POST /api/resources/{resourceId}/maintenance`: Administrador espera `201`, recebe `403`.
  - `GET /relatorios/utilizacao`: Responsável e Administrador esperam `200`, recebem `403`.
- **Risco:** A API bloqueia operações legítimas mesmo quando o token identifica um perfil autorizado.
- **Correção esperada:** Converter somente as autoridades de perfil confiáveis dos JWTs em authorities Spring, mantendo os testes com tokens reais.
