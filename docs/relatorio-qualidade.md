# Relatório de qualidade — Organização de Recursos

Formato da Aula 13. Cada afirmação traz **evidência** (onde conferir), **interpretação** (o que ela permite concluir) e **limite** (o que ela não prova). O que não foi medido está escrito como "não medido", com o motivo.

## 1. Identificação

| Item | Valor |
|---|---|
| Repositório | https://github.com/nathanfliberatti/Organizacao_de_recursos_QA |
| Commit de referência | `main` em `3e849fa` (merge do PR #13), CI verde: [execução 43141973485](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/43141973485). Este relatório entra no PR seguinte (matriz de autorização completa, mutação refeita, relatório); o commit que receber a tag `v1.0.0-rc1` é o da `main` depois desse merge, e o hash dele é o da própria tag. |
| Data | 2026-11-12 |
| Integrantes | Nathan Ferraz Liberatti (responsável por R1–R4, R9, R10) e Erick Yuji Conceição (R5–R8, R11); contas e blocos de trabalho em [decisao-qualidade.md](decisao-qualidade.md#declaração-dos-integrantes) |
| Ambiente de teste | CI: GitHub Actions, `./mvnw -B verify`, Java 21, PostgreSQL 16 via Testcontainers, SonarCloud. Local: contêiner Linux, PostgreSQL 16 sem Docker (ver [testes/mutacao-manual.md](testes/mutacao-manual.md)). Carga: máquina única local com JMeter. |
| Decisão | Liberar com ressalvas, só como *release candidate* acadêmico: [decisao-qualidade.md](decisao-qualidade.md) |

## 2. Escopo e fora de escopo

**Em escopo:** API Spring Boot (Java 21), PostgreSQL 16 e interface estática que cobrem [RF-01 a RF-15](requisitos-funcionais.md) e [RNF-01 a RNF-07](requisitos-nao-funcionais.md): cadastro e busca de recursos, reserva sem conflito de sala e professor, aprovação de recurso restrito, manutenção, ciclo de vida auditado com sete estados, materiais, notificações e relatório de utilização. Contrato em [api.md](api.md).

**Fora de escopo (não avaliado):**

| Item | Motivo |
|---|---|
| Teste de intrusão, HTTPS, limite de tentativas de login, rotação de chave | Escopo acadêmico (risco R7) |
| Desempenho em ambiente separado e com repetições | Só havia máquina única (R6) |
| Validação das regras de negócio com usuários reais | Não houve (R1, R9) |
| Acessibilidade e compatibilidade de navegadores além de Chromium | Não medido |
| Job diário de treinamento descrito em `arquitetura.md` | Não implementado (R10) |
| OpenAPI gerado | O contrato é escrito à mão e protegido por `ApiDocumentationTest` (R8) |

## 3. Estratégia por nível

| Nível | O que cobre | Ferramenta | Onde |
|---|---|---|---|
| Unidade | Regras de domínio: transições (49 pares), manutenção, intervalo, elegibilidade do Responsável | JUnit 5, testes parametrizados | `ReservationStatusTransitionTest`, `MaintenanceRuleTest`, `ReservationIntervalValidationTest` |
| API em memória | Fluxos completos, concorrência e autorização com segurança real | MockMvc | `ApplicationFlowTest`, `ReservationApiTest` |
| Integração com PostgreSQL | Restrição de exclusão, travas, transação de auditoria, relatório | Testcontainers, Failsafe (`*IT`) | `ReservationConcurrencyIT`, `ReservationAuditIT`, `ApplicationFlowIT`, `UtilizationReportApiIT` |
| Autorização | Matriz completa lida do documento: 35 linhas × 4 perfis = 140 verificações, mais travas de documento↔código | Teste parametrizado | `AuthorizationMatrixFullIT`, `AuthorizationMatrixDocumentTest`, `AuthorizationMatrixIT` |
| Comportamento (BDD) | 15 cenários de aprovação, manutenção e estados alternativos | Cucumber | `src/test/resources/features` |
| Integração externa | Webhook de notificação, inclusive falha e lentidão | WireMock | `NotificationWebhookTest`, `NotificationWebhookApiTest` |
| Interface | 3 perfis, 375 px e 1200 px | Playwright, [`e2e/e2e.js`](../e2e/e2e.js) | **Manual, fora do CI** |
| Desempenho | Latência e integridade sob disputa | JMeter | [performance/](performance/) |
| Eficácia dos testes | 21 mutações manuais nas regras críticas | Manual | [testes/mutacao-manual.md](testes/mutacao-manual.md) |
| Análise estática | Segurança, confiabilidade, manutenibilidade | SonarCloud, Quality Gate | Painel do projeto |

## 4. Resultados contra as metas

| Meta | Resultado | Situação |
|---|---|---|
| Cobertura de linhas ≥ 80% | 93,7% (1.033 de 1.103) | Atendida |
| Cobertura de branches ≥ 70% | 74% (322 de 430) | Atendida, com folga pequena |
| 100% dos requisitos críticos da RTM com evidência | 10 de 10 com risco, teste localizável, execução, mutante detectado e CI verde no PR #14 (`30bae72`) | Atendida; falta só o CI da `main` após o merge |
| Zero defeito crítico aberto | 0 defeitos de severidade Crítica registrados; 0 abertos de qualquer severidade (14 corrigidos, 1 mitigado) | Atendida |
| Zero vulnerabilidade | Sonar: Segurança A, 0 issues de segurança abertas, 0 *hotspots* | Atendida no escopo do Sonar |
| Quality Gate aprovado | Passed | Atendida |
| p95 ≤ 300 ms e erros < 1% | p95 de 33–49 ms, 0 erros inesperados | Atendida no ambiente local |

### 4.1 Cobertura

- **Evidência:** relatório JaCoCo do artifact `jacoco-report` da `main` em `1abb9da` ([execução 42921866225](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42921866225)): instruções 92% (432 de 6.013 perdidas), linhas 93,7%, branches 74% (108 de 430 perdidas), métodos 95,3%. O gate do POM falha o `verify` abaixo de 80%/70%.
- **Interpretação:** o código exercitado pelos testes, com `*IT`, passa dos limites combinados.
- **Limite:** a medição é do commit `1abb9da`, **anterior** ao PR #13 e a este. Não medi a cobertura do commit de referência; o gate passou no CI de `3e849fa`, o que garante só os mínimos de 80%/70%, não o valor. Cobertura de linha não mede a qualidade das asserções: para isso serve a mutação (4.3). Sonar reporta 88,4% por outra métrica (linhas e condições juntas).

### 4.2 Testes

- **Evidência:** no CI do PR #13 ([execução 43134310856](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/43134310856)): 177 testes Surefire + 116 Failsafe = **293, 0 falhas, 0 erros, 0 ignorados**. A `main` em `3e849fa` repete o mesmo conjunto com CI verde.
- **Neste PR:** o CI do PR #14 ([execução 43144281216](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/43144281216), commit `30bae72`) passou: Maven verify, SonarCloud e Quality Gate. O Failsafe executou **259 testes, 0 falhas, 0 erros** (116 do PR #13 + 140 da matriz completa + 3 novos); o gate do JaCoCo foi atendido. Localmente, `./mvnw -B test` deu 183 testes sem falhas; não li no log do CI a linha do Surefire.
- **Interpretação:** nenhuma regressão conhecida no conjunto atual.
- **Limite:** o valor da cobertura não foi lido neste run (só o gate). A UI (E2E, 37 verificações) roda manualmente, fora do CI ([testes/e2e-resultado.md](testes/e2e-resultado.md)).

### 4.3 Requisitos críticos e eficácia dos testes

- **Evidência:** [testes/mutacao-manual.md](testes/mutacao-manual.md): 21 mutantes nas dez regras críticas (RF-05, 06, 07, 08, 10, 13, 14, 15, RNF-02, RNF-03) mais autorização. Na primeira rodada **5 sobreviveram**: 3 eram lacunas reais (limite da manutenção, atomicidade da auditoria sem transação externa, cancelamento de reserva iniciada) e 2 eram defesas redundantes (trava de aviso e restrição de exclusão, que só falham juntas). As 3 lacunas viraram testes (commit `3f2ed8f`). Resultado final: **19 de 21 mortos (90,5%)**; os 2 restantes são os redundantes, mortos quando combinados. Dos 10 requisitos críticos, **10 (100%)** têm ao menos um mutante detectado.
- **Interpretação:** os testes dos requisitos críticos falham quando a regra quebra, e a busca achou fraquezas que a cobertura de 93,7% não mostrava.
- **Limite:** amostra de 21 mutantes escolhidos por mim, sem ferramenta automática (PIT). "100%" quer dizer que cada requisito tem uma quebra detectável, não que todo comportamento está coberto. A coluna "CI" refere-se ao PR #14; o commit da tag é o da `main` depois do merge, cujo CI precisa ser conferido de novo.

### 4.4 Análise estática e segurança

- **Evidência:** SonarCloud, `main` (commit `1abb9da`, [painel](https://sonarcloud.io/summary/overall?id=nathanfliberatti_Organizacao_de_recursos_QA)): Quality Gate Passed; Segurança A, Confiabilidade A, Manutenibilidade A; 0 *hotspots*; duplicação 0,0%; 1 issue aberta (`java:S8786`, regex em código de teste) e 1 aceita (isenção de CSRF em `PerformanceAuthConfiguration`, API stateless com token Bearer, justificada no código). O PR #8 passou depois de corrigir 6 issues (2 de confiabilidade, 4 de segurança).
- **Interpretação:** sem vulnerabilidade conhecida pela análise estática; senhas com BCrypt, sem segredo fixo no repositório, autorização por método verificada na matriz.
- **Limite:** o painel foi lido no commit `1abb9da`; não reli o painel para `3e849fa` nem para este PR. Análise estática não substitui teste de intrusão (não feito). O token fica em `sessionStorage` (exposto a XSS; mitigado pelo escape de todo texto dinâmico).

### 4.5 Autorização

- **Evidência:** [seguranca/matriz-autorizacao.md](seguranca/matriz-autorizacao.md) lista 35 linhas (31 rotas mapeadas, com cenários próprio/alheio) × 4 perfis. `AuthorizationMatrixFullIT` executa as 140 células contra a API com PostgreSQL. `AuthorizationMatrixDocumentTest` **falha** se uma rota mapeada não tem linha, se uma linha não corresponde a rota ou não tem caso de teste; verifiquei removendo a linha de `no-show` e o teste apontou `POST /api/reservations/{}/no-show`. 4 mutantes de autorização (M16–M19) foram mortos.
- **Interpretação:** nenhuma rota nova entra sem decisão explícita de quem pode usá-la.
- **Limite:** a matriz fixa o código HTTP por perfil, não a regra de negócio completa de cada resposta. A rota `/relatorios/utilizacao` só existe no perfil `postgres`; por isso o teste de documento a trata como exceção declarada.

### 4.6 Desempenho

- **Evidência:** [rodada 02](performance/resultado-02.md): 1.780.105 amostras, 0 erros inesperados, p95 de 36 ms (disponibilidade) e 49 ms (criação); na disputa pela mesma sala, 1 reserva aceita e 534.013 recusadas, conferidas no banco. [Rodada 03](performance/resultado-03.md), com 200.000 reservas no banco: p95 de 33 ms e 44 ms, 1 aceita e 255.895 recusadas. Metas aprovadas em [performance/rnf.md](performance/rnf.md).
- **Interpretação:** nessa configuração a meta de 300 ms foi atendida com larga folga e a integridade sob disputa se manteve.
- **Limite:** máquina única, uma sala disputada, sem repetição, sem rede real; não vale como garantia de produção. A rodada 01 foi descartada como inconclusiva.

## 5. Defeitos por severidade e status

Fonte: [defeitos/README.md](defeitos/README.md) (escala de severidade e prioridade definida lá).

| Severidade | Total | Corrigidos | Mitigados | Abertos |
|---|---:|---:|---:|---:|
| Crítica | 0 | 0 | 0 | 0 |
| Alta | 8 | 8 | 0 | 0 |
| Média | 5 | 4 | 1 | 0 |
| Baixa | 2 | 2 | 0 | 0 |
| **Total** | **15** | **14** | **1** | **0** |

Alta: DEF-001, 002, 003, 005, 009, 011, 014, 015. Média: DEF-004, 006, 007, 013 (corrigidos) e DEF-012 (mitigado). Baixa: DEF-008, 010.

- **Evidência:** cada defeito tem arquivo próprio com causa, correção e teste de regressão. Defeitos achados na mutação desta rodada não são defeitos de produto, e sim lacunas de teste (seção 4.3); nenhum código de produção mudou.
- **Interpretação:** nenhum defeito conhecido aberto; o único mitigado (DEF-012, perfil de carga exige credenciais por ambiente) tem procedimento documentado.
- **Limite:** o registro mostra o que foi encontrado, não o que existe. A severidade e a prioridade foram atribuídas pela equipe na entrega final, não no momento do achado.

## 6. Riscos residuais

Fonte: riscos aceitos R1–R11 em [decisao-qualidade.md](decisao-qualidade.md#riscos-aceitos) e riscos técnicos RK-01 a RK-18 em [riscos.md](riscos.md).

| Risco | Impacto | Responsável |
|---|---|---|
| R5 Revisão por pares incompleta (só o PR #13 foi aprovado antes do merge) | Defeitos podem passar sem segunda leitura | Erick |
| R6 Carga em máquina única | Capacidade real desconhecida | Erick |
| R7 Sem teste de intrusão, HTTPS, limite de login; token em `sessionStorage` | Inaceitável em produção | Erick |
| R8 E2E fora do CI; contrato escrito à mão | Regressão de tela passa até alguém rodar o E2E | Erick |
| R1 Matriz de estados não validada com usuários reais | Regra pode divergir do negócio | Nathan |
| R2 Fechamento mensal do relatório em UTC | Reservas à meia-noite podem cair no mês vizinho | Nathan |
| R4 Webhook sem reenvio | Aviso pode se perder se a API externa cair | Nathan |
| R9 Manutenção não afeta reservas já existentes | Reserva anterior ao bloqueio continua válida | Nathan |
| R3, R10, R11 | Modo em memória é uma instância só; job de treinamento ausente; issue de regex em teste | Nathan / Erick |

- **Interpretação:** o que sobra é limitação do escopo acadêmico ou decisão de negócio pendente, nada que afete a integridade das reservas.
- **Limite:** a avaliação de probabilidade e impacto é da equipe, sem validação externa.

## 7. Revisão por pares

- **Evidência:** Erick Yuji Conceição (`Erickyuji08`) aprovou o PR #13 **antes do merge** ([revisão](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pull/13#pullrequestreview-5867927078)) e o PR #9 depois do merge ([1](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pull/9#pullrequestreview-5851518916), [2](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pull/9#pullrequestreview-5851608090)). Os PRs #1–#8, #10–#12 não têm revisão humana registrada.
- **Interpretação:** a mudança maior da entrega foi lida por um segundo integrante antes de entrar.
- **Limite:** o PR #14 (este relatório) ainda não tem aprovação de Erick; o GitHub não deixa o autor aprovar o próprio PR.

## 8. Recomendação

**Liberar com ressalvas**, como *release candidate* para avaliação acadêmica e demonstração. **Não** declarar pronto para produção.

- **Por quê:** as regras críticas têm teste que falha quando a regra quebra (4.3), a autorização está verificada célula a célula (4.5), o CI e o Quality Gate passam (4.2, 4.4) e não há defeito aberto (5).
- **Condição para a tag:** aprovação de Erick no PR #14 e CI verde na `main` depois do merge (o CI do PR já passou).
- **Condição para produção:** fechar R5, R6 e R7, validar a matriz de estados (R1) com usuários reais e rodar carga em ambiente separado.

## 9. Uso de ferramentas de IA

Parte do código e da documentação foi produzida com agentes de código (GitHub Copilot e Claude Code), com commits co-assinados. A revisão e o aceite são dos integrantes.
