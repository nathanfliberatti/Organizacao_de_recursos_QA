# DEF-007: Entrada inválida de reserva causa exceção interna

- **Severidade / prioridade:** Média / P2 — Validação de entrada e mensagens de erro: robustez e exposição de detalhes.
- **Situação:** Corrigido em `c8754c6`
- **Evidência:** `AuthorizationMatrixIT.invalidInputReturnsSafeProblemDetail`
- **Operação:** `POST /api/reservations`
- **Casos divergentes:**
  - `endsAt` anterior a `startsAt` propaga `DataIntegrityViolationException` com erro SQL sobre intervalo invertido, em vez de `400 ProblemDetail`.
  - Campos temporais ausentes propagam `NullPointerException`, em vez de `400 ProblemDetail`.
- **Risco:** Erros internos escapam da API, podem expor detalhes de banco/implementação e não orientam o cliente a corrigir a requisição.
- **Correção esperada:** Validar os campos e a ordem do período antes do acesso ao store e responder `ProblemDetail` sanitizado com mensagem compreensível.
