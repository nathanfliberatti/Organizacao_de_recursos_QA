# DEF-001: Código de sucesso da criação de reserva restrita

- **Situação:** Aberto
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`
- **Operação:** `POST /api/reservations/restricted`
- **Células divergentes:** Solicitante, Responsável e Administrador esperam `201`; a API devolve `200`.
- **Risco:** Clientes não conseguem distinguir criação de leitura e podem tratar a reserva criada como uma resposta sem efeito.
- **Correção esperada:** Retornar `201 Created` nas três células autenticadas, preservando `401` para anônimo.
