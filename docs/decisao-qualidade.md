# Decisão de qualidade — `v1.0.0-rc1`

**Sistema:** Organização de Recursos · **Versão:** `v1.0.0-rc1` · **Data:** 2026-11-10
**Repositório:** https://github.com/nathanfliberatti/Organizacao_de_recursos_QA

## Decisão

**LIBERAR COM RESSALVAS** como *release candidate* para **avaliação acadêmica e demonstração**.

**Não** se declara o sistema pronto para produção: a evidência vem de testes automatizados, de uma análise estática e de uma rodada de carga em ambiente local de máquina única. Não houve teste de intrusão, validação em ambiente separado, HTTPS, nem validação de negócio com usuários reais. A liberação para produção exigiria, no mínimo, fechar os riscos R1, R5, R6 e R7 abaixo.

## Critérios, evidência e resultado

| # | Critério de liberação | Evidência (no commit da tag) | Resultado |
|---|---|---|---|
| C1 | CI verde na `main`: `./mvnw -B verify` com testes unitários, `*IT` com PostgreSQL e gate de cobertura (80% linhas / 70% branches) | Checks do commit da tag — [índice de evidências](evidencias/README.md), item 1 | Atendido (CI verde exigido antes da tag) |
| C2 | Quality Gate do SonarCloud aprovado | Painel do SonarCloud — [índice](evidencias/README.md), item 3; PR #8 passou após corrigir 6 issues | Atendido no PR #8; **[PREENCHER: confirmar o status do gate no commit da tag]** |
| C3 | Requisitos críticos com teste que falha se o comportamento quebrar: sobreposição/concorrência (RF-05, RF-06), aprovação (RF-07), manutenção (RF-08), auditoria (RF-10), ciclo de vida (RF-14) e preservação de reserva iniciada (RF-15) | [rtm.md](rtm.md); `ReservationConcurrencyIT`, `AuthorizationMatrixIT`, `ReservationAuditIT`, `ApplicationFlowTest/IT` | Atendido, **exceto RF-14 parcial** (estados alternativos não modelados) |
| C4 | Autorização por perfil verificada em todas as operações protegidas | `AuthorizationMatrixIT`, [seguranca/matriz-autorizacao.md](seguranca/matriz-autorizacao.md) | Atendido |
| C5 | Nenhum defeito conhecido em aberto | [defeitos/README.md](defeitos/README.md): 11 registrados, 11 corrigidos | Atendido |
| C6 | Desempenho do RNF-PERF-01 (p95 ≤ 300 ms, erros < 1%) e integridade sob disputa | [performance/resultado-02.md](performance/resultado-02.md): p95 36 ms e 49 ms, 0 erros, 1 reserva aceita em 534.014 tentativas | Atendido **no ambiente local descrito**; não generalizável |
| C7 | Aplicação reproduzível a partir do README em clone limpo | [entrega/checklist.md](entrega/checklist.md) | Atendido (com Docker para o `verify`) |
| C8 | Rastreabilidade requisito → teste | [rtm.md](rtm.md) | Atendido com lacunas listadas (RF-03 competência/texto, RF-13 parametrizado) |
| C9 | Revisão por pares dos PRs de entrega | [relatorio-qualidade.md §8](relatorio-qualidade.md#8-revisão-por-pares) | **Não atendido** (sem revisão humana registrada) → risco R5 |

## Riscos aceitos

| ID | Risco | Impacto | Justificativa da aceitação | Responsável |
|---|---|---|---|---|
| R1 | Estados `REJEITADA`, `NÃO_COMPARECEU` e `CANCELADA` (como estado do ciclo) do RF-14 não modelados; cancelamento é exclusão lógica | Requisito parcialmente atendido; relatórios não distinguem rejeição/ausência | A matriz de transições e de perfis nunca foi definida na documentação; implementar sem ela criaria regra de negócio inventada | **[PREENCHER: nome]** |
| R2 | Fechamento mensal do relatório em UTC | Reservas próximas à meia-noite podem cair no mês vizinho para quem usa outro fuso | Decisão de negócio pendente; a tela informa o fuso | **[PREENCHER: nome]** |
| R3 | Modo em memória não detecta sobreposição | Dupla reserva se alguém usar o modo sem banco como se fosse produção | Modo só para demonstração; o README e a documentação avisam; o perfil `postgres` é o suportado | **[PREENCHER: nome]** |
| R4 | Notificações com entrega simulada | Usuário não recebe aviso real de criação/alteração/cancelamento | O RF-11 permite entrega simulada; o evento fica registrado e consultável | **[PREENCHER: nome]** |
| R5 | Sem revisão por pares humana nos PRs de entrega | Defeitos e riscos podem passar sem segunda leitura | Prazo da entrega; CI e Quality Gate funcionam como controle automatizado parcial | **[PREENCHER: nome]** |
| R6 | Carga medida em máquina única, uma sala disputada, sem repetição | Capacidade real e variabilidade desconhecidas; desempenho em produção não comprovado | Evidência suficiente para o RNF-PERF-01 local; ampliar exigiria ambiente separado | **[PREENCHER: nome]** |
| R7 | Sem teste de segurança além da matriz de autorização; sem HTTPS, limite de tentativas de login, rotação de chave; token em `sessionStorage` | Exposição em produção (força bruta, roubo de token por XSS) | Escopo acadêmico; senhas com BCrypt, segredos fora do repositório e escape de todo texto dinâmico reduzem o risco | **[PREENCHER: nome]** |
| R8 | Interface sem E2E automatizado; contrato da API escrito à mão (sem OpenAPI gerado) | Regressões de tela e divergência entre documento e código podem passar despercebidas | Interface validada manualmente; contrato conferido contra os controllers | **[PREENCHER: nome]** |
| R9 | Manutenção cadastrada não afeta reservas já existentes (decisão pendente) | Reserva anterior ao bloqueio permanece válida | Decisão de negócio pendente em `requisitos-funcionais.md` | **[PREENCHER: nome]** |
| R10 | Job diário de treinamento descrito em `arquitetura.md` não implementado | Parte da restrição documentada sem implementação | Fora do escopo entregue; o documento informa isso | **[PREENCHER: nome]** |
| R11 | Issue `java:S8786` aberta na `main` (regex em teste) | Nenhum impacto no produto; mancha o painel do Sonar | Código de teste anterior à entrega | **[PREENCHER: nome]** |

## Declaração dos integrantes

O que cada integrante fez foi levantado **a partir do histórico de commits** da `main`. **Cada integrante deve revisar o seu bloco e confirmar** (as contas de commit são as indicadas).

**Nathan Ferraz Liberatti** (conta `Nathan Liberatti`) declara ter feito:
- commit inicial e documentação de personas, requisitos funcionais/não funcionais e arquitetura (`151faed`);
- gate de cobertura JaCoCo e publicação do relatório no CI (`47e6938`) e reconstrução do projeto sem ferramentas de IA (`c20d605`);
- restrição do escopo da isenção de CSRF (PR #5);
- histórico auditável, relatório de utilização, matriz de autorização e correção dos defeitos DEF-003 a DEF-011 (PRs #6 e #7);
- análise da rodada 01 de desempenho (`126e9f8`);
- frontend, login JWT, banco de dados, correções do Sonar (PR #8) e documentação de entrega, **com assistência de agente de código** (commits co-assinados).
**[CONFIRMAR pelo integrante]**

**Erick Yuji Conceição** (contas `Erick Yuji` e `l3ndgamer3` / L3ndGamer) declara ter feito:
- bootstrap do Maven e do CI (`6f08ba6`, PR #1);
- avaliação arquitetural (`66f4e0b`);
- execução dos cenários de aprovação/bloqueio com Cucumber (PR #3);
- correção da concorrência de reservas no PostgreSQL (PR #4, ADR-001, DEF-001 e DEF-002);
- harness local de teste de carga (`2b01f06`).
**[CONFIRMAR pelo integrante]**

Se algo acima não corresponder ao que foi feito, o integrante corrige este arquivo antes do envio.

## Texto para o formulário de entrega

> Repositório (público): https://github.com/nathanfliberatti/Organizacao_de_recursos_QA — Tag avaliada: `v1.0.0-rc1` (https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/releases/tag/v1.0.0-rc1). Integrantes: Nathan Ferraz Liberatti e Erick Yuji Conceição. Decisão de qualidade: liberar com ressalvas (release candidate para avaliação; não declarado pronto para produção) — `docs/decisao-qualidade.md`.
