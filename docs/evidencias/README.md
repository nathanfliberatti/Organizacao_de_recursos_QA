# Pacote de evidências — `v1.0.0-rc1`

Todas as evidências se referem ao **commit apontado pela tag `v1.0.0-rc1`**. Os links abaixo usam o nome da tag, que resolve para esse commit depois que ela é publicada. Se algo precisar mudar, cria-se `v1.0.0-rc2`; a tag `rc1` nunca é movida.

- Repositório: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA
- Tag: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/releases/tag/v1.0.0-rc1
- Commit da tag e seus checks: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/commit/v1.0.0-rc1

## Índice

| # | Evidência | Onde está | Observação |
|---|---|---|---|
| 1 | **Execução do CI no commit da tag** (`./mvnw -B verify` + SonarCloud) | Checks do commit: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/commit/v1.0.0-rc1 → "CI / Maven verify". Execuções do workflow: https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/actions/workflows/ci.yml | O workflow roda em `push` na `main`; o commit da tag é o do merge que o disparou. |
| 2 | **Relatório do JaCoCo** | Artifact `jacoco-report` da execução do item 1 (retido por 30 dias) e `target/site/jacoco/index.html` ao rodar `./mvnw -B verify` | Mínimos: 80% de linhas e 70% de branches (gate do POM). O artifact expira; para guardar o valor, copie-o para o relatório de qualidade. |
| 3 | **Quality Gate do SonarCloud** | https://sonarcloud.io/dashboard?id=nathanfliberatti_Organizacao_de_recursos_QA (PR do release: ver a seção "Pull requests") | O Sonar não guarda cópia no repositório; o painel é a fonte. |
| 4 | **Resultado do teste de carga** | [docs/performance/resultado-02.md](../performance/resultado-02.md) (+ `docs/performance/rodada-02/` com `statistics.json`) | Rodada 01 permanece como histórico "inconclusivo". |
| 5 | **Matriz de rastreabilidade** | [docs/rtm.md](../rtm.md) | — |
| 6 | **Registro de defeitos** | [docs/defeitos/](../defeitos/README.md) | 11 defeitos, todos corrigidos. |
| 7 | **Relatório de qualidade** | [docs/relatorio-qualidade.md](../relatorio-qualidade.md) | Dados não registrados estão indicados no texto (percentual de cobertura e notas gerais do Sonar). |
| 8 | Decisão de qualidade | [docs/decisao-qualidade.md](../decisao-qualidade.md) | — |
| 9 | Checklist da entrega em clone limpo | [docs/entrega/checklist.md](../entrega/checklist.md) | — |
| 10 | Documentação da API | [docs/api.md](../api.md) | — |
| 11 | Changelog | [CHANGELOG.md](../../CHANGELOG.md) | — |

## Como reproduzir

```bash
git clone https://github.com/nathanfliberatti/Organizacao_de_recursos_QA.git
cd Organizacao_de_recursos_QA
git checkout v1.0.0-rc1
./mvnw -B verify          # exige Docker (Testcontainers)
```

Para subir a aplicação, siga o [README](../../README.md).
