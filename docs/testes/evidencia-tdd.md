# Evidência TDD — Aprovação, manutenção e estados

## Estado do ciclo

**Em andamento.** Foram concluídos sete ciclos RED/GREEN/refactor no domínio, todos com `./mvnw -B test`. Os commits de teste e implementação são pequenos e separados. Os testes atuais são unitários JUnit 5; a cobertura de API ainda não foi criada. O código atual ainda não é uma aplicação Spring Boot.

Cada RED abaixo compilou antes de falhar por assertion. Os arquivos `target/` são saída local do Maven e não fazem parte dos commits.

## Rastreabilidade por cenário

| Cenário BDD | Commit red | Commit green | Commit refactor | Trecho da saída do teste |
|---|---|---|---|---|
| Recurso restrito não é considerado aprovado antes da decisão | `1fe463b` | `f7cb091` | `1028d72` | RED: `expected: <SOLICITADA> but was: <APROVADA>`; GREEN/refactor: `1 test, Failures: 0`. |
| Responsável aprova solicitação restrita e decisão/estado são registrados | `61d0ed6` | `77c438d` | `ac075bc` | RED: `expected: <APROVADA> but was: <SOLICITADA>` e decisão `null`; GREEN/refactor: `2 tests, Failures: 0`. |
| Solicitante não aprova a própria reserva; estado permanece inalterado | `bc5fda4` | `0c990fd` | `6be25fd` | RED: `expected: <SOLICITADA> but was: <APROVADA>`; GREEN/refactor: `3 tests, Failures: 0`. |
| Perfil Solicitante não aprova solicitação restrita de outra pessoa | `0ec56e9` | `7431973` | `fd0ca57` | RED: Solicitante distinto gerou `APROVADA`; GREEN/refactor: `6 tests, Failures: 0`. |
| Reserva com sobreposição com período de manutenção é recusada | `d11309e` | `765eee8` | `485c112` | RED: `expected: <true> but was: <false>` para manutenção ativa e `expected: <false> but was: <true>` para reserva; GREEN/refactor: `4 tests, Failures: 0`. |
| Fluxo principal `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA` é aceito e auditado | `7b6c906` | `febd500` | `9707d3d` | RED: esperado `CONCLUIDA`, observado `APROVADA`, com transições vazias; GREEN/refactor: `5 tests, Failures: 0`. |
| Aprovação repetida após `CONCLUÍDA` preserva decisão e histórico | `bea1e5b` | `390ecf2` | `1657f49` | RED: decisão mudou de `12:00` para `13:00`; GREEN/refactor: `7 tests, Failures: 0`. |
| Salto `SOLICITADA → EM_USO` é recusado e preserva o estado | Não executado em RED: a política de transição foi criada no ciclo do fluxo principal; não fabricar regressão para simular falha | Pendente de teste dedicado | Pendente | A implementação atual rejeita o salto, mas falta comprovação com teste de assertion próprio. |
| API: solicitação restrita antes da decisão e aprovação por Responsável | Pendente | Pendente | Pendente | Não executado; camada HTTP ainda não existe. |
| API: impedir autoaprovação e preservar estado | Pendente | Pendente | Pendente | Não executado; camada HTTP ainda não existe. |
| API: recurso indisponível durante manutenção | Pendente | Pendente | Pendente | Não executado; camada HTTP ainda não existe. |
| API: fluxo principal e auditoria | Pendente | Pendente | Pendente | Não executado; camada HTTP ainda não existe. |
| Reserva termina exatamente no início da manutenção | Pendente: política não definida no BDD/requisitos | Pendente | Pendente | Não executado: aguarda decisão de produto. |
| Reserva começa exatamente no fim da manutenção | Pendente: política não definida no BDD/requisitos | Pendente | Pendente | Não executado: aguarda decisão de produto. |
| Transições alternativas para `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` | Pendente: origens e atores não definidos | Pendente | Pendente | Não executado: aguarda matriz de transições aprovada. |

Saída concisa do baseline anterior à criação das fontes (não representa teste de comportamento):

```text
[INFO] No sources to compile
[INFO] No sources to compile
[INFO] No tests to run.
[INFO] BUILD SUCCESS
```

## Pendências

- Criar teste RED próprio para o salto `SOLICITADA → EM_USO` sem desfazer temporariamente o comportamento já implementado.
- Criar a aplicação e os testes de API Spring Boot 3 em ciclos individuais.
- Não testar os limites exatos de manutenção até definição da política.
- Não testar as alternativas `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` até aprovação da matriz de transições.
- Manter `Clock` injetado; não usar `Instant.now()` oculto nem `Thread.sleep`.
