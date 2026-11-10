# DEF-010: Código de sucesso da criação de reserva restrita

- **Renumerado:** registrado originalmente como `DEF-001` (commit `7d3c96b` e mensagens de commit da época citam `DEF-001`); renumerado para `DEF-010` por colisão com [DEF-001](DEF-001-concorrencia-reservas.md).
- **Situação:** Corrigido em `7d3c96b`
- **Evidência:** `AuthorizationMatrixIT.apiMatchesAuthorizationMatrix`
- **Operação:** `POST /api/reservations/restricted`
- **Células divergentes:** Solicitante, Responsável e Administrador esperam `201`; a API devolve `200`.
- **Risco:** Clientes não conseguem distinguir criação de leitura e podem tratar a reserva criada como uma resposta sem efeito.
- **Correção esperada:** Retornar `201 Created` nas três células autenticadas, preservando `401` para anônimo.
