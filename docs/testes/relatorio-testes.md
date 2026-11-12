# Relatório de testes — `v1.0.0-rc1`

Consolida os testes da entrega final: o que existe, o que foi executado, com que resultado e como cada exigência do [RNF-07](../requisitos-nao-funcionais.md) foi atendida. O plano está em [plano-tdd.md](plano-tdd.md); a rastreabilidade requisito → teste, em [../rtm.md](../rtm.md).

## 1. Resultado da execução

| Execução | Resultado | Onde |
|---|---|---|
| CI da `main`, commit `75294e9` (`./mvnw -B verify`) | **158 testes, 0 falhas**: 55 Surefire + 103 Failsafe; gate JaCoCo atendido; Quality Gate do SonarCloud aprovado | [run 42931118129](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42931118129); detalhe por classe no job "Maven verify" |
| Esta entrega (testes adicionados, §4) | Surefire passa de 55 para **64**; o Failsafe ganha 1 teste (`searchesResourcesByCompetencyAndFreeText` roda também em `ApplicationFlowIT`) | CI do PR da entrega final e da `main` após o merge |
| E2E da interface (manual) | **28/28** verificações em 3 execuções seguidas com PostgreSQL; 27/27 sem banco | [e2e-resultado.md](e2e-resultado.md) |
| Carga (JMeter, local) | 1.780.105 amostras, 0 erros inesperados | [../performance/resultado-02.md](../performance/resultado-02.md) |

Reproduzir: `./mvnw -B test` (sem Docker, só os testes unitários e de API em memória) ou `./mvnw -B verify` (completo, exige Docker para os `*IT`).

## 2. Testes por nível

| Nível | Classe (testes no commit `75294e9`) | O que verifica |
|---|---|---|
| Unidade / domínio | `ReservationLifecycleTest` (6), `ReservationApprovalTest` (4), `ReservationAuditEntryTest` (4), `MaintenanceRuleTest` (5), `InMemoryReservationStoreTest` (1), `InMemoryResourceStoreTest` (1), `InMemoryReservationAuditServiceTest` (4), `PostgresReservationStoreTest` (5, com mocks) | Ciclo de vida, aprovação sem autoaprovação, auditoria, regra de manutenção, stores |
| API caixa-preta (MockMvc, sem banco) | `ReservationApiTest` (4), `ReservationAuditApiTest` (5), `PerformanceAuthControllerTest` (3), `ApplicationFlowTest` (7) | Contratos HTTP, perfis, fluxo completo com login JWT real |
| BDD (Cucumber) | `RunCucumberTest` (6 cenários de [`aprovacao-bloqueio.feature`](../../src/test/resources/features/aprovacao-bloqueio.feature)) | Aprovação e bloqueio de recursos restritos e em manutenção |
| Integração com PostgreSQL real (Testcontainers) | `AuthorizationMatrixIT` (69), `ReservationConcurrencyIT` (11), `ApplicationFlowIT` (9), `ReservationAuditIT` (7), `UtilizationReportApiIT` (7) | Matriz de autorização, concorrência, persistência/auditoria, relatório, fluxo completo |
| Concorrência | `ReservationConcurrencyIT` (20 requisições simultâneas, 5 repetições por cenário, sala e professor), `ApplicationFlowIT#concurrentRequestsForSameSlotConfirmOnlyOneReservation` (8 requisições) | No máximo uma reserva aceita por disputa |
| E2E da interface | [`e2e/e2e.js`](../../e2e/e2e.js) (28 verificações) | Fluxos principais no navegador; **executado manualmente, fora do CI** |
| Desempenho | [`perf/reservas.jmx`](../../perf/reservas.jmx) | RNF-PERF-01 |

## 3. Exigências do RNF-07

| Exigência | Estado | Evidência / observação |
|---|---|---|
| Testes unitários JUnit 5 **incluindo parametrizados** | Atendido | `ReservationIntervalValidationTest` (6 casos de intervalo), `AuthorizationMatrixIT` (matriz perfil × operação, 69 casos), demais em §2 |
| Testes de integração | Atendido | Cinco classes `*IT` em PostgreSQL real |
| API caixa-preta | Atendido | `ApplicationFlowTest` / `ApplicationFlowIT` usam apenas HTTP e login real |
| End-to-end | **Parcial** | Script Playwright versionado e executado, mas **fora do CI** |
| Testcontainers para persistência | Atendido | Todos os `*IT` |
| WireMock para integrações externas críticas | **Não aplicável** | O sistema **não tem integração externa**: notificações são simuladas e gravadas no banco, e não há chamada HTTP de saída. Se uma API real de notificação for adotada, o WireMock passa a ser necessário. |
| Teste automatizado de concorrência contra dupla reserva | Atendido | `ReservationConcurrencyIT`, `ApplicationFlowIT`, carga rodada 02 (1 aceita, 534.013 recusadas) |
| Evidência de pelo menos uma funcionalidade em TDD/BDD | Atendido | Ciclos RED/GREEN/refactor em [evidencia-tdd.md](evidencia-tdd.md) e feature BDD com 6 cenários ([bdd-aprovacao-bloqueio.md](bdd-aprovacao-bloqueio.md)) |
| GitHub Actions em PR, JaCoCo, SonarCloud | Atendido | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) |
| Cobertura ≥ 80% de linhas e ≥ 70% de branches | Atendido | 93,7% de linhas e 74% de branches (JaCoCo no CI, `75294e9`) |
| Exclusões do JaCoCo explícitas e documentadas | Atendido | **Nenhuma exclusão configurada** (`pom.xml` sem `<excludes>`); toda a cobertura é medida sobre todo o código |
| Zero bugs críticos e zero vulnerabilidades críticas na entrega final | Atendido | Critério adotado: severidade **High/Blocker** no SonarCloud. Na `main` (análise de `1abb9da`): 0 vulnerabilidades abertas, 1 bug aberto de severidade **Medium** (`java:S8786`, em código de teste), 0 hotspots. Ver [../relatorio-qualidade.md](../relatorio-qualidade.md) §4. |

## 4. Testes acrescentados na entrega final

Cada um fecha uma lacuna registrada na [RTM](../rtm.md):

| Teste | Lacuna |
|---|---|
| `ReservationIntervalValidationTest` (parametrizado, 6 casos) | RF-13: término igual, anterior e posterior ao início; exigência de testes parametrizados |
| `AbstractApplicationFlowTest#searchesResourcesByCompetencyAndFreeText` | RF-03: pesquisa por competência e por texto livre (roda em memória e em PostgreSQL) |
| `ApiDocumentationTest` | RNF-05: todo endpoint mapeado no código precisa constar em [api.md](../api.md); foi verificado que o teste **falha** quando um endpoint é removido da documentação |
| [`e2e/e2e.js`](../../e2e/e2e.js) | RNF-04 e RNF-07 (E2E) |

## 5. Defeitos que os testes encontraram

11 defeitos em [../defeitos/](../defeitos/README.md), todos com teste de regressão. Na etapa final, a revisão e a execução encontraram outros dois, registrados como `DEF-012` e `DEF-013`.

## 6. Limitações

- O E2E não roda no CI, não cobre todas as telas e não mede acessibilidade.
- A carga e a concorrência foram medidas em máquina única.
- Os testes em memória não detectam sobreposição de horários; só os `*IT` em PostgreSQL garantem isso.
- [mutacao-manual.md](mutacao-manual.md) registra a auditoria manual de mutações feita antes desta entrega; seus números (137 testes, CI 0/10) são os daquele momento.
