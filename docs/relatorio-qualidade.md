# Relatório final de qualidade — Organização de Recursos

**Versão avaliada:** `v1.0.0-rc1` (commit da tag; antes dela, `main` em `d0447b0`) · **Data:** 2026-11-10
**Autores:** Nathan Ferraz Liberatti, Erick Yuji Conceição

> **Como ler este relatório.** Cada afirmação aponta para uma evidência no repositório ou no CI. Nada foi estimado: o que não foi medido ou não foi registrado aqui está dito como tal.

## 1. Escopo e resumo

Sistema web para organizar a alocação de salas, professores e materiais com prevenção de conflitos de horário: API Spring Boot (Java 21), PostgreSQL 16 e frontend estático. Requisitos: [RF-01 a RF-15](requisitos-funcionais.md) e [RNF-01 a RNF-07](requisitos-nao-funcionais.md); contrato da API em [api.md](api.md).

**Conclusão resumida:** os requisitos críticos (sobreposição e concorrência, autorização por perfil, ciclo de vida auditado, preservação de reservas iniciadas) têm teste automatizado que passa no CI; não há defeito conhecido em aberto. Permanecem lacunas de escopo (estados alternativos do RF-14, notificação real) e de evidência (revisão por pares, E2E da interface, carga em ambiente separado). A decisão e os riscos aceitos estão em [decisao-qualidade.md](decisao-qualidade.md).

## 2. Testes

| Nível | Ferramenta | Resultado | Evidência |
|---|---|---|---|
| Unitário / API em memória / BDD | JUnit 5, MockMvc, Cucumber | **55 testes, 0 falhas** (Surefire) | `./mvnw -B test`; artifact `test-reports` do CI |
| Integração com PostgreSQL real | Testcontainers (Failsafe, classes `*IT`) | Passam no CI | Execução do CI do commit da tag ([índice de evidências](evidencias/README.md)) |
| Concorrência | `ReservationConcurrencyIT` (20 requisições, 5 repetições por cenário) e `ApplicationFlowIT` (8 requisições) | Exatamente uma reserva aceita por disputa | idem |
| Autorização | `AuthorizationMatrixIT` (matriz perfil × operação) | Passa | [seguranca/matriz-autorizacao.md](seguranca/matriz-autorizacao.md) |
| Fluxo ponta a ponta da API | `ApplicationFlowTest` (memória) e `ApplicationFlowIT` (PostgreSQL) | Passam | [rtm.md](rtm.md) |
| Interface (E2E) | Playwright, script versionado [`e2e/e2e.js`](../e2e/e2e.js), 3 perfis, 375 px e 1200 px | **28/28 verificações**, 3 execuções seguidas com PostgreSQL; 27/27 sem banco | [testes/e2e-resultado.md](testes/e2e-resultado.md). **Executado manualmente, fora do CI** |

Plano e histórico TDD/BDD: [testes/plano-tdd.md](testes/plano-tdd.md), [testes/evidencia-tdd.md](testes/evidencia-tdd.md), [testes/mutacao-manual.md](testes/mutacao-manual.md).

**Total de testes do `verify` no CI** (job "Maven verify" do PR #9, commit `078e029`, [execução 42728214539](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42728214539)): **55 testes Surefire + 103 testes Failsafe = 158, 0 falhas, 0 erros, 0 ignorados.** Os `*IT` incluem `AuthorizationMatrixIT` (69), `ReservationConcurrencyIT` (11), `ApplicationFlowIT` (9), `UtilizationReportApiIT` (7) e `ReservationAuditIT` (7).

## 3. Cobertura

- Gate do POM: **80% de linhas e 70% de branches** (JaCoCo; falha o `verify` se não atingir).
- Medição local, somente testes unitários, no commit `d0447b0`: **90,7% de linhas** (sem os `*IT`).
- **Cobertura total do CI (com `*IT`)**, lida no relatório JaCoCo (`index.html` do artifact `jacoco-report`) da execução do CI da `main` no commit `1abb9da` ([run 42921866225](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42921866225)):

| Contador | Cobertura | Perdidos / total |
|---|---:|---|
| Instruções | **92%** | 432 de 6.013 |
| Linhas | **93,7%** (calculado: 1.033 de 1.103) | 70 de 1.103 |
| Branches | **74%** | 108 de 430 |
| Métodos | 95,3% (calculado: 323 de 339) | 16 de 339 |

  O gate do POM (80% de linhas e 70% de branches) foi atendido (`All coverage checks have been met.` no log do CI). O Sonar reporta 88,4% de cobertura geral, porque calcula por outra métrica (linhas e condições juntas).

## 4. Análise estática e segurança

- **SonarCloud:** o Quality Gate do PR #8 **passou** após corrigir 6 issues de código novo (2 de confiabilidade — `Random` reutilizável; 4 de segurança — SQL montado por concatenação). No PR #9 o gate também passou (0 issues novas, 0 issues aceitas, 0 security hotspots).
- **SonarCloud, `main` (Overall Code), análise do commit `1abb9da`** (https://sonarcloud.io/summary/overall?id=nathanfliberatti_Organizacao_de_recursos_QA):

| Medida | Valor |
|---|---|
| Quality Gate | **Passed** |
| Segurança | **A** — 0 issues abertas |
| Confiabilidade | **A** — 1 issue aberta (a regex `java:S8786` abaixo) |
| Manutenibilidade | **A** — 0 issues abertas |
| Issues aceitas | 1 (isenção de CSRF em `PerformanceAuthConfiguration`, aceita no painel) |
| Cobertura | 88,4% (1,1 mil linhas a cobrir) |
| Duplicação | 0,0% (3,6 mil linhas) |
| Security hotspots | 0 |
| Tamanho | ~3 mil linhas de código |

- **Issue aberta na `main`:** `java:S8786` (regex com backtracking) em `AuthorizationMatrixIT.java:367`, código de teste, anterior à entrega final.
- **Autenticação e autorização:** JWT HS256, perfil por claim, autorização por método e matriz testada. Senhas com BCrypt; login com comparação de hash também para usuário inexistente.
- **Segredos:** nenhum segredo fixo no repositório (`DB_PASSWORD` obrigatória; `JWT_SECRET` e senha de demonstração aleatórios quando ausentes).
- **CSRF:** isento em `/api/**` porque a API é stateless e autenticada só por header Bearer; marcado com `NOSONAR java:S4502` e justificado no código.
- **Não avaliado:** teste de intrusão, HTTPS/terminação TLS, limitação de tentativas de login, rotação de chaves e armazenamento do token em `sessionStorage` (exposto a XSS; mitigado por escape de todo texto dinâmico no frontend).

## 5. Defeitos

13 defeitos registrados em [defeitos/](defeitos/README.md) (concorrência, deadlock, matriz de autorização, validação, posse de reservas, perfil de carga e isolamento de teste): **12 corrigidos e 1 mitigado** (DEF-012, por procedimento documentado). Os corrigidos têm teste de regressão. Defeitos encontrados na entrega final: (a) IDs `DEF-001`/`DEF-002` duplicados — renumerados para `DEF-010`/`DEF-011`; (b) perfil `perf,postgres` não subia sem credenciais após o PR #8 — [DEF-012](defeitos/DEF-012-perfil-perf-postgres-sem-credenciais.md); (c) `ReservationConcurrencyIT` reiniciava ids sem limpar o ciclo de vida — [DEF-013](defeitos/DEF-013-concorrencia-it-ids-colidem-com-ciclo-de-vida.md).

## 6. Desempenho

[Rodada 02](performance/resultado-02.md): 1.780.105 amostras, 0 erros inesperados; p95 de 36 ms (consulta de disponibilidade) e 49 ms (criação) contra meta de 300 ms; 1 reserva aceita e 534.013 recusadas na disputa pela mesma sala, confirmadas no banco. **Ambiente local de máquina única, uma sala disputada; não vale como garantia de produção.** [Rodada 03](performance/resultado-03.md), com 200.000 reservas já no banco: 853.042 amostras, 0 erros, p95 de 33 ms e 44 ms, 1 aceita e 255.895 recusadas. As metas (p95 ≤ 300 ms, erros < 1%, 50 usuários) foram aprovadas pelo grupo em [performance/rnf.md](performance/rnf.md). A rodada 01 ficou inconclusiva.

## 7. Rastreabilidade

[rtm.md](rtm.md) liga cada requisito a cenário e teste. Cobertura de requisitos: ver [status-requisitos.md](status-requisitos.md). Lacunas fechadas na entrega final: pesquisa por competência e texto (RF-03), teste parametrizado de intervalo (RF-13), E2E (RNF-04, fora do CI) e verificação automática de que a API está documentada (`ApiDocumentationTest`, RNF-05). Lacunas que permanecem: estados alternativos do RF-14 e ausência de OpenAPI gerado (o contrato é escrito à mão, mas protegido pelo teste anterior).

## 8. Revisão por pares

O repositório usa pull requests. **Erick Yuji Conceição** (conta GitHub `Erickyuji08`, colaborador) **aprovou o PR #9** — o PR de entrega final — em duas revisões com estado `APPROVED` sobre o commit `dda4229`, registradas no GitHub em 2026-11-11: [revisão 1](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pull/9#pullrequestreview-5851518916) e [revisão 2](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pull/9#pullrequestreview-5851608090). **As aprovações foram feitas depois do merge** (o PR #9 foi mergeado em 2026-11-10), portanto não funcionaram como bloqueio prévio.

Sem revisão humana registrada: os PRs #1 a #8 (o #8 só recebeu comentários de bots) e o PR #10 (somente documentação, mergeado após o CI verde).

## 9. Uso de ferramentas de IA

Parte do código e da documentação foi produzida com assistência de agentes de código (GitHub Copilot e Claude Code), com commits co-assinados. A responsabilidade pela revisão e aceite é dos integrantes.

## 10. Limitações e riscos

Listados com impacto e responsável em [decisao-qualidade.md](decisao-qualidade.md#riscos-aceitos).
