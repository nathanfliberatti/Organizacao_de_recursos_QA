# Pacote de evidências — `v1.0.0-rc1`

Todas as evidências se referem ao **commit apontado pela tag `v1.0.0-rc1`**. Os links abaixo usam o nome da tag, que resolve para esse commit depois que ela é publicada. Se algo precisar mudar, cria-se `v1.0.0-rc2`; a tag `rc1` nunca é movida.

- Repositório: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA
- Tag: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/releases/tag/v1.0.0-rc1
- Commit da tag e seus checks: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/commit/v1.0.0-rc1

## Índice

| # | Evidência | Onde está | Observação |
|---|---|---|---|
| 1 | **Execução do CI no commit da tag** (`./mvnw -B verify` + SonarCloud) | Checks do commit: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/commit/v1.0.0-rc1 → "CI / Maven verify". Execuções do workflow: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/workflows/ci.yml | O workflow roda em `push` na `main`; o commit da tag é o do merge que o disparou. |
| 2 | **Relatório do JaCoCo** | Artifact `jacoco-report` da execução do item 1 (retido por 30 dias) e `target/site/jacoco/index.html` ao rodar `./mvnw -B verify` | Mínimos: 80% de linhas e 70% de branches (gate do POM). Valores lidos (commit `1abb9da`): 93,7% de linhas, 74% de branches, 92% de instruções — copiados em [relatorio-qualidade.md §4.1](../relatorio-qualidade.md). O artifact expira em 30 dias. |
| 3 | **Quality Gate do SonarCloud** | https://sonarcloud.io/dashboard?id=nathanfliberatti_Organizacao_de_recursos_QA (PR do release: ver a seção "Pull requests") | O Sonar não guarda cópia no repositório; o painel é a fonte. |
| 4 | **Resultado do teste de carga** | [resultado-02.md](../performance/resultado-02.md) (banco vazio) e [resultado-03.md](../performance/resultado-03.md) (200 mil reservas), com `statistics.json` em `docs/performance/rodada-02/` e `rodada-03/` | Rodada 01 permanece como histórico "inconclusivo". |
| 5 | **Matriz de rastreabilidade** | [docs/rtm.md](../rtm.md) | — |
| 6 | **Registro de defeitos** | [docs/defeitos/](../defeitos/README.md) | 15 defeitos priorizados: 14 corrigidos e 1 mitigado (DEF-012). |
| 7 | **Relatório de qualidade** | [docs/relatorio-qualidade.md](../relatorio-qualidade.md) | Dados não registrados estão indicados no texto (percentual de cobertura e notas gerais do Sonar). |
| 8 | Decisão de qualidade | [docs/decisao-qualidade.md](../decisao-qualidade.md) | — |
| 9 | Checklist da entrega em clone limpo | [docs/entrega/checklist.md](../entrega/checklist.md) | — |
| 10 | Documentação da API | [docs/api.md](../api.md) | Protegida por `ApiDocumentationTest` |
| 12 | Arquitetura do sistema implementado (C4 e sequências) | [docs/arquitetura-implementada.md](../arquitetura-implementada.md), imagens em [docs/diagramas/](../diagramas/) | — |
| 13 | Relatório de testes e E2E | [docs/testes/relatorio-testes.md](../testes/relatorio-testes.md), [docs/testes/e2e-resultado.md](../testes/e2e-resultado.md) | — |
| 14 | Apresentação (slides) e roteiro da demonstração | [docs/apresentacao/apresentacao.pptx](../apresentacao/apresentacao.pptx), [docs/apresentacao/roteiro-demonstracao.md](../apresentacao/roteiro-demonstracao.md) | 13 slides com notas do apresentador |
| 11 | Changelog | [CHANGELOG.md](../../CHANGELOG.md) | — |

## Capturas versionadas

Cópias em [imagens/](imagens/), porque os artifacts do CI expiram (30 dias) e o Sonar não guarda histórico no repositório. Cada print traz, na própria imagem, o commit ou o PR a que se refere.

| Arquivo | O que mostra | Referência |
|---|---|---|
| [jacoco-total-main-1abb9da.png](imagens/jacoco-total-main-1abb9da.png) | Relatório JaCoCo, linha Total: 92% de instruções, 74% de branches, 1.103 linhas (70 perdidas) | CI da `main`, commit `1abb9da` |
| [sonarcloud-main-overall-1abb9da.png](imagens/sonarcloud-main-overall-1abb9da.png) | SonarCloud `main` (Overall Code): Quality Gate Passed, Segurança/Confiabilidade/Manutenibilidade A, cobertura 88,4%, duplicação 0,0% | Análise do commit `1abb9da` |
| [sonarcloud-pr9-quality-gate.png](imagens/sonarcloud-pr9-quality-gate.png) | PR #9: Quality Gate passed (0 issues novas, 0 hotspots) e checks verdes | PR #9 |
| [ci-maven-verify-pr9.png](imagens/ci-maven-verify-pr9.png) | Execução "Maven verify" do PR #9 com todos os passos verdes (análise Sonar, upload do JaCoCo) | [run 42728214539](https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/runs/42728214539) |
| [github-aprovacoes-pr9.png](imagens/github-aprovacoes-pr9.png) | Duas aprovações de `Erickyuji08` no PR #9 | PR #9 |
| [sonarcloud-pr8-issues-parte1.png](imagens/sonarcloud-pr8-issues-parte1.png), [parte2](imagens/sonarcloud-pr8-issues-parte2.png) | As 6 issues do SonarCloud no PR #8 **antes da correção** (2 de confiabilidade, 4 de segurança) | PR #8 |
| [e2e-recursos-admin.png](imagens/e2e-recursos-admin.png), [e2e-reservas-solicitante.png](imagens/e2e-reservas-solicitante.png), [e2e-estados-alternativos.png](imagens/e2e-estados-alternativos.png), [e2e-mobile.png](imagens/e2e-mobile.png) | Telas capturadas pelo E2E (Administrador, Solicitante, estados REJEITADA/NÃO COMPARECEU/CANCELADA, 375 px) | [e2e-resultado.md](../testes/e2e-resultado.md) |

## Como reproduzir

```bash
git clone https://github.com/nathanfliberatti/Organizacao_de_recursos_QA.git
cd Organizacao_de_recursos_QA
git checkout v1.0.0-rc1
./mvnw -B verify          # exige Docker (Testcontainers)
```

Para subir a aplicação, siga o [README](../../README.md).
