# DEF-001 — API de reservas não persiste nem serializa concorrência

- **Data:** 2026-10-25
- **Severidade:** Alta — permite confirmar solicitações incompatíveis e não persiste a reserva
- **Requisitos:** RF-05, RF-06, RNF-02
- **Estado:** Resolvido em `87afce8` (base de persistência `26ccea4`)

## Pré-condições

- Docker acessível ao Testcontainers.
- `./mvnw -B -Dit.test=ReservationConcurrencyIT verify`.
- PostgreSQL 16-alpine iniciado pelo Testcontainers.

## Passos para reproduzir

1. Preparar 20 solicitações com a mesma sala `room-shared`, o mesmo intervalo `2026-10-25T10:00:00Z–2026-10-25T11:00:00Z` e professores diferentes.
2. Fazer as 20 threads aguardarem em `CountDownLatch` e liberar todas simultaneamente.
3. Enviar cada solicitação pela API `POST /api/reservations`.
4. Contar respostas HTTP 201 e HTTP 409.
5. Consultar PostgreSQL para a sala e o intervalo disputados.
6. Repetir cinco vezes; repetir os mesmos passos com sala diferente por solicitação e professor `professor-shared`.

## Evidência

A execução RED inicial de 2026-10-25 com `ReservationConcurrencyIT` falhou nos dez ciclos (cinco de sala e cinco de professor). Em cada ciclo, as assertions observaram **0 respostas HTTP 201**, **0 respostas HTTP 409** e **0 linhas correspondentes no PostgreSQL**, em vez de uma resposta aceita, 19 conflitos e uma linha.

Causa identificada no código anterior à correção: `ReservationSchedulingService` gravava somente em `ReservationCatalog` em memória, sem escrita no DataSource; o endpoint devolvia HTTP 200 com um booleano e não havia constraint/lock que arbitrasse solicitações concorrentes.

Após a correção, `ReservationConcurrencyIT` passou todas as cinco repetições por recurso para sala e as cinco para professor: cada repetição verificou 1 resposta 201, 19 respostas 409 e uma linha correspondente por consulta SQL.

## Critério de encerramento atingido

- Cada uma das cinco repetições para sala e para professor resulta em exatamente 1 resposta 201, 19 respostas 409 e exatamente uma linha consultada no PostgreSQL para a chave de recurso e intervalo disputados: **PASS**.
- O gate segue usando os mínimos JaCoCo existentes, sem exclusões ou redução de meta.
