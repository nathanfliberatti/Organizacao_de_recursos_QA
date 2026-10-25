# DEF-002 — Deadlock durante disputas simultâneas por sala/professor

- **Data:** 2026-10-25
- **Severidade:** Alta — uma solicitação concorrente recebe erro 500 em vez de HTTP 409
- **Requisitos:** RF-05, RF-06, RNF-02
- **Estado:** Resolvido em `87afce8`

## Pré-condições

- PostgreSQL 16 real via Testcontainers.
- Constraints `EXCLUDE USING gist` simultâneas para sala/intervalo e professor/intervalo.
- 20 requests em paralelo para recurso/intervalo conflitante.

## Passos para reproduzir

1. Executar `./mvnw -B verify` sem filtro.
2. Na disputa de professor, enviar 20 reservas para `professor-shared` no mesmo intervalo, cada uma associada a uma sala diferente.
3. Liberar as threads simultaneamente pelo `CountDownLatch`.
4. Observar responses e consultar a tabela PostgreSQL para aquele professor/intervalo.

## Evidência

A execução completa inicial de 2026-10-25 alcançou o PostgreSQL, mas uma das cinco repetições da disputa por professor falhou com `org.springframework.dao.PessimisticLockingFailureException`, SQLSTATE `40P01`: `ERROR: deadlock detected`, enquanto o PostgreSQL verificava a constraint de exclusão na tabela `reservations`. O erro escapou da API em vez de ser uma resposta HTTP 409.

A disputa por sala passou. A falha surgiu porque a tabela contém duas constraints GIST concorrentes (sala + `tstzrange` e professor + `tstzrange`), cujas verificações podem adquirir locks em ordem incompatível durante inserts concorrentes.

## Correção aplicada

Serializar inserts por professor e sala com advisory transaction locks em ordem determinística antes da consulta e do insert. Manter ambas as exclusion constraints como autoridade final de integridade e traduzir colisões/abortos de conflito para HTTP 409. A decisão está registrada em [ADR-001](../adr/ADR-001-integridade-concorrente-postgresql.md).

## Critério de encerramento atingido

Cada uma das cinco repetições para sala e cinco para professor terminou sem erro de infraestrutura/transação: exatamente uma resposta 201, 19 respostas 409 e uma linha correspondente no PostgreSQL: **PASS**.
