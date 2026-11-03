# DEF-002: Autoridades dos perfis não são extraídas do JWT

- **Situação:** Corrigido em `5639986`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`
- **Causa observada:** JWTs válidos e assinados com a claim `roles` autenticam, mas não produzem as autoridades `ROLE_*` esperadas por `@PreAuthorize`.
- **Células divergentes:**
  - `GET /api/reservations/{id}/history`: Responsável e Administrador esperam `200`, recebem `403`.
  - `POST /api/resources/{resourceId}/maintenance`: Administrador espera `201`, recebe `403`.
  - `GET /relatorios/utilizacao`: Responsável e Administrador esperam `200`, recebem `403`.
- **Risco:** A API bloqueia operações legítimas mesmo quando o token identifica um perfil autorizado.
- **Correção esperada:** Converter somente as autoridades de perfil confiáveis dos JWTs em authorities Spring, mantendo os testes com tokens reais.
