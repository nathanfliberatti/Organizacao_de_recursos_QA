# ADR-001 — Integridade concorrente de reservas no PostgreSQL

- **Status:** Aceito
- **Data:** 2026-10-25
- **Contexto:** RF-05/RF-06 exigem impedir sobreposição de sala e professor sob concorrência. O teste com 20 solicitações paralelas demonstrou ausência de persistência; após adicionar duas exclusion constraints, a disputa pelo mesmo professor gerou `40P01` durante a verificação GIST.

## Decisão

Usar PostgreSQL como autoridade de integridade, com duas constraints `EXCLUDE USING gist` sobre igualdade do recurso e sobreposição de `tstzrange(starts_at, ends_at)`: uma para `room_id` e outra para `professor_id`. Habilitar `btree_gist` para combinar igualdade textual com o operador de sobreposição de ranges.

Antes de verificar disponibilidade e inserir, `PostgresReservationStore` adquire advisory transaction locks para professor e sala em ordem lexical determinística. A transação contém lock, consulta e insert. As exclusion constraints continuam ativas como garantia final mesmo se um escritor não adquirir advisory locks. O serviço converte colisão `23P01` e deadlock transacional `40P01` em conflito HTTP `409`; demais erros de persistência são propagados.

Os ranges usam o padrão PostgreSQL `[)` do construtor `tstzrange`. A política de negócio para contatos exatos ainda está aberta em RF-05/RF-08; os testes concorrentes não dependem desses contatos e a decisão funcional deve ser confirmada antes de adicionar expectativas de fronteira.

## Alternativas consideradas

- **Somente consulta prévia:** rejeitada; duas transações podem observar ausência de conflito e inserir simultaneamente.
- **Somente exclusion constraints:** rejeitada como solução completa para a forma atual da tabela; duas constraints diferentes apresentaram deadlock sob inserção simultânea para o mesmo professor.
- **Lock pessimista de linha:** não adotado porque o lock exigiria uma linha estável de recurso/professor, inclusive antes da primeira reserva. Advisory locks permitem serializar pela chave lógica sem inventar esse registro-base.

## Consequências

- O banco impede duas confirmações sobrepostas mesmo fora do caminho HTTP da aplicação.
- Solicitações concorrentes pelo mesmo recurso lógico serializam por advisory lock e colisões retornam `409`.
- Hash collisions de `hashtextextended` podem serializar recursos distintos desnecessariamente, mas não permitem dupla reserva.
- Testes de integração requerem PostgreSQL real via Testcontainers e permissão para criar `btree_gist` no banco de teste.
- Validação: `ReservationConcurrencyIT#onlyOneConcurrentReservationIsAcceptedForSameRoomAndTime` e `#onlyOneConcurrentReservationIsAcceptedForSameProfessorAndTime`, cinco repetições cada; `./mvnw -B verify` passa com JaCoCo em 91,9% de linhas e 77,3% de branches.

## Referências

- Defeitos: [DEF-001](../defeitos/DEF-001-concorrencia-reservas.md), [DEF-002](../defeitos/DEF-002-deadlock-constraints-exclusao.md)
- Implementação inicial/schema: commit `26ccea4`; serialização, regressão do deadlock e cobertura: commit `87afce8`.
