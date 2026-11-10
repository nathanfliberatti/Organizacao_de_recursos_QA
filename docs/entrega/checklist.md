# Checklist da entrega — avaliação em clone limpo

**Commit avaliado:** `d0447b0` (`main`, merge do PR #8) · **CI desse commit:** [run 42720000610 — success](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42720000610)
**Data:** 2026-11-09 · **Método:** `git clone` do zero em diretório temporário, seguindo apenas o `README.md`. Nada foi corrigido nesta etapa.

## 1. Passos executados no clone limpo

| Passo | Resultado | Observação |
|---|---|---|
| `git clone` do repositório público | OK | HEAD = `d0447b0` |
| `./mvnw -B verify` | **Parcial** | 55 testes unitários (Surefire) passaram. Os `*IT` (Failsafe) falharam com `Could not find a valid Docker environment`: o ambiente da avaliação não tinha Docker. No CI (que tem Docker) o `verify` passa. **O README não declara Docker como pré-requisito do `verify`.** |
| Subir a aplicação (perfil `postgres`, variáveis `DB_PASSWORD`, `JWT_SECRET`, `APP_DEMO_*`) | OK | `docker compose` não pôde ser usado (sem Docker); usado um PostgreSQL 16 local com o mesmo usuário/banco. A aplicação subiu, `GET /` → 200 e `POST /api/auth/login` → 200 com a senha de demonstração. |
| Documentação da API (`/v3/api-docs`) | Não existe | → 401 (sem OpenAPI); não há outro documento que descreva a API completa. |

## 2. Entregáveis

Legenda — **Existe:** sim / parcial / não · **Atualizado:** sim / não / n.a.

| Item da especificação | Existe | Atualizado | Caminho | Observação |
|---|---|---|---|---|
| Repositório com revisão por pares | parcial | — | PRs [#1 a #8](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/pulls?q=is%3Apr) | O PR #8 só tem comentário de bot (GitHub Advanced Security); **nenhuma revisão humana registrada**. |
| Aplicação executável | sim | sim | `README.md` (seção "Executando a aplicação"), `docker-compose.yml` | Validado no clone limpo (item 1). |
| README | parcial | parcial | `README.md` | Falta: Docker/Testcontainers como pré-requisito do `verify`; link para documentação da API; seção de desempenho/qualidade. |
| Documentação da API | **não** | n.a. | (resumo de endpoints em `docs/frontend-e-banco-de-dados.md`) | Sem OpenAPI nem documento de contrato (corpos, códigos de status, perfis). |
| `docs/rtm.md` | sim | **não** | `docs/rtm.md` | Cita 18 testes; marca RF-02, RF-03, RF-09, RF-11, RF-13 como "sem cenário", mas hoje há cobertura em `ApplicationFlowTest`/`ApplicationFlowIT`. |
| Diagrama de contexto | sim | **não** | `docs/arquitetura.md` §10 (C4 Contexto) | Escrito antes da implementação; não registra frontend estático, login JWT nem novas tabelas. |
| Diagrama de componentes | sim | **não** | `docs/arquitetura.md` §10 (C4 Componentes) | Idem. |
| Sequências críticas | sim | parcial | `docs/arquitetura.md` §11 | Fluxo online de reserva existe; falta login e aprovação automática/restrita como implementados. |
| Plano de testes | sim | parcial | `docs/testes/plano-tdd.md` | — |
| Relatório de testes | parcial | **não** | `docs/testes/evidencia-tdd.md` | Diz "Em andamento" e 18 + 11 testes; hoje são 55 unitários + ITs. Há também `mutacao-manual.md` e `bdd-aprovacao-bloqueio.md`. |
| Pipeline de CI | sim | sim | `.github/workflows/ci.yml` | Verde na `main` (run acima). |
| Relatório do SonarCloud | parcial | sim | [Quality Gate do PR #8](https://sonarcloud.io/dashboard?id=nathanfliberatti_Organizacao_de_recursos_QA&pullRequest=8) | Não há cópia versionada; o link do painel é a evidência. |
| Relatório do JaCoCo | parcial | sim | artifact `jacoco-report` de cada run do CI | **`retention-days: 3`**: o artifact expira em 3 dias e some antes da correção. |
| Plano de teste de carga | sim | parcial | `perf/reservas.jmx`, `docs/performance/rnf.md` | O plano usa o login do perfil `perf`, não o login real; precisa de revisão. |
| Resultado do teste de carga | sim | **não** | `docs/performance/resultado-01.md` | Decisão **"inconclusivo"**: o `.jtl` bruto e o relatório do JMeter não estão versionados. |
| Registro de defeitos | sim | parcial | `docs/defeitos/` (11 arquivos) | IDs duplicados: `DEF-001` (2 arquivos) e `DEF-002` (2 arquivos). |
| Relatório final de qualidade | **não** | n.a. | (esperado: `docs/relatorio-qualidade.md`) | O "relatório de qualidade da Aula 13" não está versionado. |
| Estado dos requisitos | sim | **não** | `docs/status-requisitos.md` | Descreve gates já resolvidos (ex.: "aplicação não inicializa no perfil padrão") e marca RF-02/03/09/11 como ausentes. |
| `CHANGELOG.md` | não | n.a. | — | Criado na etapa 2. |
| `docs/evidencias/README.md`, `docs/decisao-qualidade.md` | não | n.a. | — | Criados nas etapas 2 e 3. |

## 3. Bloqueadores, em ordem

1. **Relatório final de qualidade ausente** (`docs/relatorio-qualidade.md`) — é o pré-requisito da aula e citado no pacote de evidências.
2. **Documentação da API ausente** — sem contrato de endpoints, corpos, códigos e perfis.
3. **RTM, status dos requisitos, relatório de testes e arquitetura desatualizados** em relação ao código entregue (RF-02, 03, 04, 09, 11, 13 e o frontend/login).
4. **Resultado do teste de carga inconclusivo** (sem `.jtl`/relatório) e plano de carga desalinhado com o login real.
5. **README sem o pré-requisito Docker** para `./mvnw -B verify` e sem apontar para a documentação da API.
6. **Evidências perecíveis:** artifact JaCoCo com retenção de 3 dias; nenhuma cópia do relatório do SonarCloud no repositório.
7. **Revisão por pares sem registro humano** nos PRs recentes.
8. **IDs duplicados** no registro de defeitos (`DEF-001`, `DEF-002`).

## 4. Pendências conhecidas que não bloqueiam, mas devem entrar como riscos aceitos

- Estados `REJEITADA`, `CANCELADA` (como estado do ciclo) e `NÃO_COMPARECEU` (RF-14) não modelados.
- Fuso do fechamento mensal do relatório é UTC (decisão de negócio pendente).
- Armazenamento em memória não detecta sobreposição de horários (somente desenvolvimento).
- Issue aberta na `main` no SonarCloud: regex com backtracking em `AuthorizationMatrixIT.java:367` (`java:S8786`), código de teste.
- Isenção de CSRF em `/api/**` marcada com `NOSONAR` (API stateless por Bearer).

## 5. Estado após as correções (PR de entrega final)

| # | Bloqueador | Situação | Onde |
|---|---|---|---|
| 1 | Relatório final de qualidade ausente | Corrigido (com campos `[PREENCHER]` para dados que só a equipe tem) | [../relatorio-qualidade.md](../relatorio-qualidade.md) |
| 2 | Documentação da API ausente | Corrigido | [../api.md](../api.md) |
| 3 | RTM, status, relatório de testes e arquitetura desatualizados | Corrigido | [../rtm.md](../rtm.md), [../status-requisitos.md](../status-requisitos.md), [../testes/evidencia-tdd.md](../testes/evidencia-tdd.md), [../arquitetura.md](../arquitetura.md) |
| 4 | Resultado de carga inconclusivo | Corrigido: rodada 02 refeita com JMeter; resumo versionado | [../performance/resultado-02.md](../performance/resultado-02.md) |
| 5 | README sem pré-requisito Docker nem link da API | Corrigido | [../../README.md](../../README.md) |
| 6 | Artifacts efêmeros (3 dias) | Corrigido: retenção de 30 dias; índice de evidências | [../evidencias/README.md](../evidencias/README.md) |
| 7 | Revisão por pares sem registro humano | **Não corrigido** — depende de um integrante revisar; registrado como risco R5 | [../decisao-qualidade.md](../decisao-qualidade.md) |
| 8 | IDs de defeito duplicados | Corrigido: `DEF-010` e `DEF-011`, com índice | [../defeitos/README.md](../defeitos/README.md) |

Achados adicionais durante a correção: o perfil `perf,postgres` deixou de subir sem `DB_USER`/`DB_PASSWORD` após o PR #8 (documentado em [../performance/rnf.md](../performance/rnf.md)); rodar a carga só com `perf` usa memória e produz resultado inválido.
