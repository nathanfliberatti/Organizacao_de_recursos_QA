# Decisão de qualidade — `v1.0.0-rc1`

**Sistema:** Organização de Recursos · **Versão:** `v1.0.0-rc1` · **Data:** 2026-11-10
**Repositório:** https://github.com/nathanfliberatti/Organizacao_de_recursos_QA

## Decisão

**LIBERAR COM RESSALVAS** como *release candidate* para **avaliação acadêmica e demonstração**.

**Não** se declara o sistema pronto para produção: a evidência vem de testes automatizados, de uma análise estática e de uma rodada de carga em ambiente local de máquina única. Não houve teste de intrusão, validação em ambiente separado, HTTPS, nem validação de negócio com usuários reais. A liberação para produção exigiria, no mínimo, fechar os riscos R5, R6 e R7 abaixo e validar a matriz de estados (R1) com usuários reais.

## Critérios, evidência e resultado

| # | Critério de liberação | Evidência (no commit da tag) | Resultado |
|---|---|---|---|
| C1 | CI verde na `main`: `./mvnw -B verify` com testes unitários, `*IT` com PostgreSQL e gate de cobertura (80% linhas / 70% branches) | Checks do commit da tag — [índice de evidências](evidencias/README.md), item 1 | Atendido: 177 testes Surefire + 116 Failsafe (293), 0 falhas no CI do PR #13; JaCoCo 93,7% de linhas e 74% de branches (mínimos 80% e 70%). CI verde na `main` exigido antes da tag. |
| C2 | Quality Gate do SonarCloud aprovado | Painel do SonarCloud — [índice](evidencias/README.md), item 3; PR #8 passou após corrigir 6 issues | Atendido: passou nos PRs #8 e #9 e na `main` (análise do commit `1abb9da`: Segurança A, Confiabilidade A, Manutenibilidade A, 0 hotspots, 88,4% de cobertura, 0,0% de duplicação). |
| C3 | Requisitos críticos com teste que falha se o comportamento quebrar: sobreposição/concorrência (RF-05, RF-06), aprovação (RF-07), manutenção (RF-08), auditoria (RF-10), ciclo de vida (RF-14) e preservação de reserva iniciada (RF-15) | [rtm.md](rtm.md); `ReservationConcurrencyIT`, `AuthorizationMatrixIT`, `ReservationAuditIT`, `ApplicationFlowTest/IT` | Atendido, **inclusive RF-14 completo** (estados alternativos implementados, matriz de 49 pares testada) |
| C4 | Autorização por perfil verificada em todas as operações protegidas | `AuthorizationMatrixIT`, [seguranca/matriz-autorizacao.md](seguranca/matriz-autorizacao.md) | Atendido |
| C5 | Nenhum defeito conhecido em aberto | [defeitos/README.md](defeitos/README.md): 15 registrados e priorizados: 14 corrigidos e 1 mitigado por procedimento documentado (DEF-012) | Atendido |
| C6 | Desempenho do RNF-PERF-01 (p95 ≤ 300 ms, erros < 1%) e integridade sob disputa | [performance/resultado-02.md](performance/resultado-02.md) (banco vazio) e [resultado-03.md](performance/resultado-03.md) (200 mil reservas): p95 de 36/49 ms e 33/44 ms, 0 erros, 1 reserva aceita em cada disputa | Atendido **no ambiente local descrito**; não generalizável |
| C7 | Aplicação reproduzível a partir do README em clone limpo | [entrega/checklist.md](entrega/checklist.md) | Atendido (com Docker para o `verify`) |
| C8 | Rastreabilidade requisito → teste | [rtm.md](rtm.md) | Atendido, com coluna de risco ([riscos.md](riscos.md)); lacuna restante: ausência de OpenAPI gerado |
| C9 | Revisão por pares dos PRs de entrega | [relatorio-qualidade.md §8](relatorio-qualidade.md#8-revisão-por-pares) | **Parcialmente atendido**: o PR #9 foi aprovado por Erick Yuji Conceição (2 revisões `APPROVED` no GitHub, posteriores ao merge); PRs #1 a #8 e #10 sem revisão humana registrada → risco R5 |

## Riscos aceitos

| ID | Risco | Impacto | Justificativa da aceitação | Responsável |
|---|---|---|---|---|
| R1 | ~~Estados alternativos do RF-14 não modelados~~ **Encerrado na entrega final** | Estados `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU` implementados com auditoria e testes ([DEF-015](defeitos/DEF-015-estados-alternativos-ausentes.md)). **Resta** a matriz de transições adotada não ter sido validada com usuários reais, e não haver regra de tempo para registrar "não compareceu" | Decisão adotada pelo grupo na entrega final, a partir da especificação | Nathan Ferraz Liberatti |
| R2 | Fechamento mensal do relatório em UTC | Reservas próximas à meia-noite podem cair no mês vizinho para quem usa outro fuso | Decisão de negócio pendente; a tela informa o fuso | Nathan Ferraz Liberatti |
| R3 | ~~Modo em memória não detecta sobreposição~~ **Encerrado na entrega final** | O modo em memória agora também impede sobreposição, de forma atômica ([DEF-014](defeitos/DEF-014-modo-em-memoria-permite-dupla-reserva.md)). **Resta**: vale para uma única instância e os dados somem ao parar | Só para demonstração; o perfil `postgres` é o suportado | Nathan Ferraz Liberatti |
| R4 | Notificações simuladas por padrão; o webhook opcional não reenvia em caso de falha | Usuário pode não receber o aviso se a API externa estiver fora do ar | O RF-11 permite entrega simulada ou integração externa; o evento fica registrado e consultável, e a integração externa (webhook) foi testada com WireMock, com falha isolada | Nathan Ferraz Liberatti |
| R5 | Revisão por pares incompleta: só o PR #9 tem aprovação humana registrada, e feita após o merge; PRs #1 a #8 e #10 sem revisão registrada | Defeitos e riscos podem passar sem segunda leitura | Prazo da entrega; CI e Quality Gate funcionam como controle automatizado parcial | Erick Yuji Conceição |
| R6 | Carga medida em máquina única, uma sala disputada, sem repetição | Capacidade real e variabilidade desconhecidas; desempenho em produção não comprovado | Evidência suficiente para o RNF-PERF-01 local; ampliar exigiria ambiente separado | Erick Yuji Conceição |
| R7 | Sem teste de segurança além da matriz de autorização; sem HTTPS, limite de tentativas de login, rotação de chave; token em `sessionStorage` | Exposição em produção (força bruta, roubo de token por XSS) | Escopo acadêmico; senhas com BCrypt, segredos fora do repositório e escape de todo texto dinâmico reduzem o risco | Erick Yuji Conceição |
| R8 | E2E da interface fora do CI (execução manual); contrato da API escrito à mão, sem OpenAPI gerado | Regressão de tela pode passar até alguém rodar o E2E; documento e código podem divergir em detalhes que o teste não vê (corpos, códigos de status) | O E2E está versionado e passou 28/28; `ApiDocumentationTest` falha se um endpoint deixar de constar em `docs/api.md` | Erick Yuji Conceição |
| R9 | Manutenção cadastrada não afeta reservas já existentes (decisão pendente) | Reserva anterior ao bloqueio permanece válida | Decisão de negócio pendente em `requisitos-funcionais.md` | Nathan Ferraz Liberatti |
| R10 | Job diário de treinamento descrito em `arquitetura.md` não implementado | Parte da restrição documentada sem implementação | Fora do escopo entregue; o documento informa isso | Nathan Ferraz Liberatti |
| R11 | Issue `java:S8786` aberta na `main` (regex em teste) | Nenhum impacto no produto; mancha o painel do Sonar | Código de teste anterior à entrega | Erick Yuji Conceição |

## Declaração dos integrantes

O que cada integrante fez foi levantado **a partir do histórico de commits** da `main`. As contas de commit são as indicadas. O grupo confirmou os blocos em 2026-11-10.

**Nathan Ferraz Liberatti** (conta `Nathan Liberatti`) declara ter feito:
- commit inicial e documentação de personas, requisitos funcionais/não funcionais e arquitetura (`151faed`);
- gate de cobertura JaCoCo e publicação do relatório no CI (`47e6938`) e reconstrução do projeto sem ferramentas de IA (`c20d605`);
- restrição do escopo da isenção de CSRF (PR #5);
- histórico auditável, relatório de utilização, matriz de autorização e correção dos defeitos DEF-003 a DEF-011 (PRs #6 e #7);
- análise da rodada 01 de desempenho (`126e9f8`);
- frontend, login JWT, banco de dados, correções do Sonar (PR #8) e documentação de entrega, **com assistência de agente de código** (commits co-assinados).
Confirmado pelo grupo em 2026-11-10 (informado por Nathan Ferraz Liberatti).

**Erick Yuji Conceição** (contas `Erick Yuji` e `l3ndgamer3` / L3ndGamer) declara ter feito:
- bootstrap do Maven e do CI (`6f08ba6`, PR #1);
- avaliação arquitetural (`66f4e0b`);
- execução dos cenários de aprovação/bloqueio com Cucumber (PR #3);
- correção da concorrência de reservas no PostgreSQL (PR #4, ADR-001, DEF-001 e DEF-002);
- harness local de teste de carga (`2b01f06`).
Confirmado pelo grupo em 2026-11-10 (informado por Nathan Ferraz Liberatti).

Distribuição dos riscos aceitos: 6 com Nathan (R1, R2, R3, R4, R9, R10) e 5 com Erick (R5, R6, R7, R8, R11), definida pelo grupo.

## Texto para o formulário de entrega

> Repositório (público): https://github.com/nathanfliberatti/Organizacao_de_recursos_QA — Tag avaliada: `v1.0.0-rc1` (https://github.com/nathanfliberatti/Organizacao_de_recursos_QA/releases/tag/v1.0.0-rc1). Integrantes: Nathan Ferraz Liberatti e Erick Yuji Conceição. Decisão de qualidade: liberar com ressalvas (release candidate para avaliação; não declarado pronto para produção) — `docs/decisao-qualidade.md`.
