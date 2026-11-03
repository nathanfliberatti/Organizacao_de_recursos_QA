# DEF-006: Cadastro de recurso e validação de capacidade ausentes

- **Situação:** Corrigido em `16fb9ae`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix` e `AuthorizationMatrixIT.invalidInputReturnsSafeProblemDetail`
- **Operação:** `POST /api/resources`, payload com `id` e `capacity`.
- **Células divergentes:** Solicitante e Responsável esperam `403`, Administrador espera `201`; os três recebem `404`.
- **Validações divergentes:** Capacidade negativa e campo `capacity` ausente esperam `400 ProblemDetail`; ambos recebem `404`.
- **Risco:** Recursos não podem ser cadastrados pela API e a capacidade inválida não é rejeitada com um erro compreensível.
- **Correção esperada:** Implementar cadastro protegido para Administrador e validar capacidade positiva e campos obrigatórios.
