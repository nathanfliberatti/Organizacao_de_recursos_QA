# Evidência TDD — Aprovação, manutenção e estados

## Estado do ciclo

**Em andamento.** Foram concluídos ciclos RED/GREEN/refactor de domínio/API e seis cenários Cucumber-JVM executáveis por MockMvc. `./mvnw -B verify` passou com 18 testes (12 JUnit + 6 Cucumber) e JaCoCo em 93,1% de linhas e 72,5% de branches. A aplicação usa Spring Boot 3.4.5; os catálogos atuais são em memória e não representam persistência definitiva.

Cada RED executado nesta tabela compilou antes de falhar por assertion. Os arquivos `target/` são saída local do Maven e não fazem parte dos commits.

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
| API: solicitação restrita permanece pendente; decisão de Responsável é aceita | `f4a88d5` | `c065ebe` | `7a3cb69` | RED: JSON esperado `SOLICITADA`, recebido `APROVADA`; GREEN/refactor: `1 API test, Failures: 0`. O fluxo aprovado por Responsável distinto também aparece no cenário do ciclo principal. |
| API: impedir autoaprovação e preservar estado | `e6ac19c` | `8dc3b60` | `dc9ed75` | RED: JSON esperado `SOLICITADA`, recebido `APROVADA`; GREEN/refactor: `2 API tests, Failures: 0`. Tentativas anteriores com CSRF ausente/rota ausente não foram tratadas como RED. |
| API: recurso indisponível durante manutenção | `2042544` | `9ae46c1` | `77f29ad` | RED: `$.accepted expected false but was true`; GREEN/refactor: `3 API tests, Failures: 0`, catálogo inalterado. |
| API: fluxo principal e auditoria | `3e7d061` | `4f4a449` | `142688b` | RED: `$.status expected EM_USO but was APROVADA`; GREEN/refactor: `4 API tests, Failures: 0`, três transições retornadas. |
| Salto `SOLICITADA → EM_USO` é recusado e preserva o estado | Não executado em RED: a política de transição já estava implementada no ciclo do fluxo principal | Pendente de teste dedicado | Pendente | O código rejeita o salto, mas não foi demonstrado por assertion independente; não foi fabricado um RED removendo comportamento já validado. |
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

- Cobrir o salto `SOLICITADA → EM_USO` com um RED próprio; a regra já foi implementada antes do teste dedicado.
- Definir persistência fora dos catálogos em memória antes de tratar a API como aplicação pronta para produção.
- Não testar os limites exatos de manutenção até definição da política.
- Não testar as alternativas `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` até aprovação da matriz de transições.
- Manter `Clock` injetado; não usar `Instant.now()` oculto nem `Thread.sleep`.
