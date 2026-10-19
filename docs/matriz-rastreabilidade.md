# Matriz de Rastreabilidade de Requisitos (RTM)

## Objetivo

Relacionar requisitos documentados a estratégias de validação e evidências, para permitir verificar a cobertura de escopo e comprovar qualidade. A fonte dos requisitos é [Requisitos Funcionais](requisitos-funcionais.md) e [Requisitos Não Funcionais](requisitos-nao-funcionais.md); os perfis são definidos em [Personas](personas/).

## Convenções

- **Crítico:** regra explicitamente listada pelo professor em “Regras críticas — Tempo e concorrência” ou “Estados e auditoria”.
- **Obrigatório:** requisito obrigatório do projeto que não foi classificado como crítico nas regras acima.
- **Planejada:** evidência ainda precisa ser produzida; não significa implementação concluída.
- Os tipos de teste nesta matriz são estratégias requeridas, não nomes de classes ou arquivos ainda existentes.

## Requisitos Funcionais

| ID | Criticidade | Verificação planejada | Evidência a registrar | Estado |
|---|---|---|---|---|
| RF-01 | Obrigatório | Testes de autorização por perfil na API em caixa-preta | Resultados de acesso permitido e negado para cada perfil | Planejada |
| RF-02 | Obrigatório | Testes de integração e API em caixa-preta para cadastro e consulta | Dados de teste e respostas observadas para sala, professor e material | Planejada |
| RF-03 | Obrigatório | Testes parametrizados de pesquisa por tipo, capacidade, localização, competência e disponibilidade | Casos, critérios consultados e resultados obtidos | Planejada |
| RF-04 | Obrigatório | Testes de integração, API em caixa-preta e E2E de criação, alteração e cancelamento | Fluxos executados e estado final da reserva | Planejada |
| RF-05 | **Crítico** | Testes parametrizados e de integração para sobreposição de sala, material e professor | Casos de intervalo conflitante e não conflitante, com resultados | Planejada |
| RF-06 | **Crítico** | Teste automatizado concorrente com persistência em Testcontainers | Execução simultânea reproduzível demonstrando no máximo uma reserva conflitante aceita | Planejada |
| RF-07 | **Crítico** | Testes de autorização e fluxo de aprovação restrita pela API | Tentativa sem aprovação, decisão por Responsável autorizado e auditoria da decisão | Planejada |
| RF-08 | **Crítico** | Teste de integração com bloqueio de manutenção | Tentativa de reserva durante manutenção rejeitada e consulta indicando indisponibilidade | Planejada |
| RF-09 | Obrigatório | Teste de integração e E2E de retirada e devolução | Registros de retirada/devolução e estados consultados | Planejada |
| RF-10 | **Crítico** | Teste de integração de auditoria para mudanças e transições de estado | Registros com ação, objeto, usuário responsável e data/hora | Planejada |
| RF-11 | Obrigatório | Testes de integração com WireMock se houver API externa, ou teste do mecanismo simulado | Evidência de notificação para criação, alteração e cancelamento | Planejada |
| RF-12 | Obrigatório | Teste de relatório mensal com dados conhecidos | Relatório conferido para utilização, horas alocadas e conflitos evitados | Planejada |
| RF-13 | **Crítico** | Testes unitários JUnit 5 parametrizados para início e término | Casos de término anterior, igual e posterior ao início | Planejada |
| RF-14 | **Crítico** | Testes de integração e API para o ciclo de vida e estados alternativos | Transições exercitadas e estados resultantes | Planejada |
| RF-15 | **Crítico** | Teste de integração e API tentando apagar reserva em `EM_USO` | Operação negada e reserva preservada para consulta/auditoria | Planejada |

## Requisitos Não Funcionais

| ID | Criticidade | Verificação planejada | Evidência a registrar | Estado |
|---|---|---|---|---|
| RNF-01 | Obrigatório | Testes de autorização na API e análise de segurança | Resultados por perfil e relatório do SonarCloud | Planejada |
| RNF-02 | **Crítico** | Teste concorrente de integração com Testcontainers | Logs/resultados de execuções simultâneas sem dupla reserva | Planejada |
| RNF-03 | **Crítico** | Testes de integração de auditoria | Registros de mudanças e transições com autoria e data/hora | Planejada |
| RNF-04 | Obrigatório | E2E e verificação responsiva dos fluxos principais e erros | Evidências em diferentes dimensões de tela e resultados dos casos de erro | Planejada |
| RNF-05 | Obrigatório | Revisão da documentação da API ou dos fluxos públicos contra o sistema | Documentação publicada/versionada e conferida | Planejada |
| RNF-06 | Obrigatório | Plano e execução de carga JMeter considerando todos os aspectos requeridos | Relatório com usuários concorrentes, duração, volume de dados, tempo de resposta e taxa de erro | Planejada |
| RNF-07 | Obrigatório | GitHub Actions em pull requests, JaCoCo, SonarCloud e evidência de TDD/BDD | Workflows, cobertura de linhas/branches, análise estática e registro da técnica aplicada | Planejada |

## Gates de Aceitação

- **Rastreabilidade crítica:** todos os requisitos marcados como Crítico devem permanecer vinculados a validação e evidência; o gate exige 100% desses requisitos na RTM.
- **Cobertura:** JaCoCo deve reportar pelo menos 80% de linhas e 70% de branches no projeto. Toda exclusão deve ser explícita e justificada.
- **Entrega final:** não pode haver bugs críticos nem vulnerabilidades críticas abertas.
- **Concorrência:** o teste automatizado deve comprovar que solicitações conflitantes simultâneas não confirmam mais de uma reserva.
- **Evidências:** percentuais de cobertura não substituem a comprovação comportamental dos requisitos.

## Pendências para Fechar a RTM

- Registrar transições permitidas e perfis autorizados para iniciar, concluir, cancelar e marcar `NÃO_COMPARECEU`, sem contrariar as personas.
- Definir os valores-alvo JMeter para usuários concorrentes, duração, volume de dados, tempo de resposta e taxa de erro.
- Definir e justificar as exclusões JaCoCo.
- Definir a fonte de classificação de bugs e vulnerabilidades críticas para o gate da entrega final.
- Definir fórmulas do relatório mensal e o fuso horário usado para determinar o mês.
- Registrar se notificações serão simuladas ou usarão API externa; os eventos de reserva, alteração e cancelamento já estão definidos.