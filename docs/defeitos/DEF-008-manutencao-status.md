# DEF-008: Código de sucesso incorreto no cadastro de manutenção

- **Situação:** Aberto
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`, após corrigir o mapeamento de JWT em DEF-002.
- **Operação:** `POST /api/resources/{resourceId}/maintenance`
- **Célula divergente:** Administrador autorizado espera `201`; recebe `200`.
- **Risco:** O cliente não recebe o código que representa a criação do período de manutenção.
- **Correção esperada:** Retornar `201 Created` para Administrador e manter `401`/`403` para os demais perfis.
