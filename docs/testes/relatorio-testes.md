# Relatório de testes — `v1.0.0-rc1`

Consolida os testes da entrega final: o que existe, o que foi executado, com que resultado e como cada exigência do [RNF-07](../requisitos-nao-funcionais.md) foi atendida. O plano está em [plano-tdd.md](plano-tdd.md); a rastreabilidade requisito → teste, em [../rtm.md](../rtm.md).

## 1. Resultado da execução

| Execução | Resultado | Onde |
|---|---|---|
| CI da `main`, commit `75294e9` (`./mvnw -B verify`) | **158 testes, 0 falhas**: 55 Surefire + 103 Failsafe; gate JaCoCo atendido; Quality Gate do SonarCloud aprovado | [run 42931118129](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42931118129); detalhe por classe no job "Maven verify" |
| CI do PR de conformidade com a especificação (PR #13, [run 43134310856](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/43134310856)) | **293 testes, 0 falhas**: **177 Surefire + 116 Failsafe**; gate JaCoCo atendido; Quality Gate do SonarCloud aprovado. Inclui estados alternativos, modo em memória sem dupla reserva, WireMock e 15 cenários Cucumber. Antes (PR #12): 64 + 104 = 168 | job "Maven verify" do run |
| E2E da interface (manual) | **28/28** verificações em 3 execuções seguidas com PostgreSQL; 27/27 sem banco | [e2e-resultado.md](e2e-resultado.md) |
| Carga (JMeter, local) | Rodada 02: 1.780.105 amostras; rodada 03 (200 mil reservas no banco): 853.042 amostras; 0 erros inesperados nas duas | [resultado-02](../performance/resultado-02.md), [resultado-03](../performance/resultado-03.md) |

Reproduzir: `./mvnw -B test` (sem Docker, só os testes unitários e de API em memória) ou `./mvnw -B verify` (completo, exige Docker para os `*IT`).

## 2. Testes por nível

| Nível | Classe (testes no CI do PR #13) | O que verifica |
|---|---|---|
| Unidade / domínio | `ReservationLifecycleTest` (6), `ReservationApprovalTest` (4), `ReservationAuditEntryTest` (4), `MaintenanceRuleTest` (5), `InMemoryReservationStoreTest` (11, parametrizado e com concorrência), `InMemoryResourceStoreTest` (1), `InMemoryReservationAuditServiceTest` (6), `PostgresReservationStoreTest` (5, com mocks), **`ReservationStatusTransitionTest` (60, matriz de 49 pares)**, **`ReservationAlternativeStatesTest` (10)**, **`NotificationWebhookTest` (14, WireMock)** | Ciclo de vida e estados alternativos, aprovação sem autoaprovação, auditoria, regra de manutenção, sobreposição em memória, stores, integração externa de notificação |
| API caixa-preta (MockMvc, sem banco) | `ReservationApiTest` (4), `ReservationAuditApiTest` (5), `PerformanceAuthControllerTest` (3), `ApplicationFlowTest` (14), `ReservationIntervalValidationTest` (6), `ApiDocumentationTest` (2), **`NotificationWebhookApiTest` (2, WireMock)** | Contratos HTTP, perfis, fluxo completo com login JWT real, documentação da API |
| BDD (Cucumber) | `RunCucumberTest` (15): 6 cenários de [`aprovacao-bloqueio.feature`](../../src/test/resources/features/aprovacao-bloqueio.feature) e 9 de [`estados-alternativos.feature`](../../src/test/resources/features/estados-alternativos.feature) (15 no total) | Aprovação e bloqueio de recursos restritos e em manutenção; rejeição, cancelamento e não comparecimento (RF-14, RF-15) |
| Integração com PostgreSQL real (Testcontainers) | `AuthorizationMatrixIT` (77), `ReservationConcurrencyIT` (11), `ApplicationFlowIT` (14), `ReservationAuditIT` (7), `UtilizationReportApiIT` (7) = 116 | Matriz de autorização, concorrência, persistência/auditoria, relatório, fluxo completo |
| Concorrência | `ReservationConcurrencyIT` (20 requisições simultâneas, 5 repetições por cenário, sala e professor), `concurrentRequestsForSameSlotConfirmOnlyOneReservation` (8 requisições, em memória e em PostgreSQL), `InMemoryReservationStoreTest` (16 em memória) | No máximo uma reserva aceita por disputa |
| E2E da interface | [`e2e/e2e.js`](../../e2e/e2e.js) (28 verificações) | Fluxos principais no navegador; **executado manualmente, fora do CI** |
| Desempenho | [`perf/reservas.jmx`](../../perf/reservas.jmx) | RNF-PERF-01 |

## 3. Exigências do RNF-07

| Exigência | Estado | Evidência / observação |
|---|---|---|
| Testes unitários JUnit 5 **incluindo parametrizados** | Atendido | `ReservationIntervalValidationTest` (6 casos de intervalo), `AuthorizationMatrixIT` (matriz perfil × operação, 69 casos), demais em §2 |
| Testes de integração | Atendido | Cinco classes `*IT` em PostgreSQL real |
| API caixa-preta | Atendido | `ApplicationFlowTest` / `ApplicationFlowIT` usam apenas HTTP e login real |
| End-to-end | **Parcial** | Script Playwright versionado, com **37 verificações** em 3 execuções seguidas (inclui os estados alternativos), mas **fora do CI** |
| Testcontainers para persistência | Atendido | Todos os `*IT` |
| WireMock para integrações externas críticas | **Atendido** | A integração externa é o **webhook opcional de notificações** (RF-11). `NotificationWebhookTest` (14 casos: cada evento e o corpo JSON, respostas 4xx/5xx, lentidão com timeout, servidor fora do ar, URLs inválidas) e `NotificationWebhookApiTest` (ponta a ponta: eventos chegam à API externa e a falha dela **nunca** bloqueia a reserva) usam WireMock. Uma mutação (tratar qualquer resposta como sucesso) fez 5 testes falharem. Por padrão a entrega continua simulada. |
| Teste automatizado de concorrência contra dupla reserva | Atendido | `ReservationConcurrencyIT`, `ApplicationFlowIT`, carga rodada 02 (1 aceita, 534.013 recusadas) |
| Evidência de pelo menos uma funcionalidade em TDD/BDD | Atendido | Ciclos RED/GREEN/refactor em [evidencia-tdd.md](evidencia-tdd.md) para aprovação e bloqueio, e duas features BDD (15 cenários) descritas em [bdd-aprovacao-bloqueio.md](bdd-aprovacao-bloqueio.md); a feature de estados alternativos foi escrita como cenários Gherkin, **sem registro de ciclos RED/GREEN separados** |
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
| `ReservationStatusTransitionTest` (49 pares), `ReservationAlternativeStatesTest`, `InMemoryReservationAuditServiceTest` (cancelamento), três fluxos em `AbstractApplicationFlowTest`, linhas novas em `AuthorizationMatrixIT`, [`estados-alternativos.feature`](../../src/test/resources/features/estados-alternativos.feature) | RF-14 e RF-15: estados alternativos e matriz de transições (DEF-015) |
| `InMemoryReservationStoreTest` (parametrizado, concorrência), sobreposição e concorrência da API também em memória | RF-05, RF-06: dupla reserva no modo em memória (DEF-014) |
| `NotificationWebhookTest`, `NotificationWebhookApiTest` (WireMock) | RF-11 e RNF-07: integração externa |

## 5. Defeitos que os testes encontraram

15 defeitos priorizados em [../defeitos/](../defeitos/README.md). Os 11 primeiros têm teste de regressão; na etapa final a execução do CI e da carga encontrou `DEF-013` (isolamento do `ReservationConcurrencyIT`, corrigido) e `DEF-012` (perfil de carga sem credenciais, mitigado por procedimento), e a comparação com a especificação encontrou `DEF-014` (dupla reserva em memória) e `DEF-015` (estados alternativos), ambos corrigidos com teste de regressão.

## 6. Limitações

- O E2E não roda no CI, não cobre todas as telas e não mede acessibilidade.
- A carga e a concorrência foram medidas em máquina única.
- O modo em memória também impede sobreposição (DEF-014), mas só vale para uma instância; a garantia entre instâncias e a persistência são verificadas apenas pelos `*IT` em PostgreSQL.
- [mutacao-manual.md](mutacao-manual.md) registra a auditoria manual de mutações feita antes desta entrega; seus números (137 testes, CI 0/10) são os daquele momento.
