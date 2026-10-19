# Requisitos Não Funcionais

## Objetivo

Registrar atributos de qualidade e entregáveis transversais necessários para avaliar a aplicação. Estes requisitos complementam os comportamentos definidos em [Requisitos Funcionais](requisitos-funcionais.md).

Os critérios quantitativos não informados permanecem pendentes de definição; não devem ser presumidos durante a implementação ou a avaliação.

## Requisitos

### RNF-01 — Segurança e controle de acesso

O sistema deve proteger as operações e informações conforme a autenticação e as permissões por perfil definidas em [RF-01](requisitos-funcionais.md#rf-01--autenticação-e-autorização-por-perfil).

**Evidência:** testes documentados de acesso autorizado e negado para os perfis configurados, incluindo tentativas de executar ações sem permissão.

**Critério quantitativo:** mecanismos específicos de autenticação, política de sessão e requisitos de segurança adicionais ainda não foram definidos.

### RNF-02 — Integridade sob concorrência

O sistema deve preservar a consistência das reservas quando solicitações concorrentes disputarem os mesmos recursos ou horários, conforme [RF-05](requisitos-funcionais.md#rf-05--detecção-de-sobreposição-de-horários) e [RF-06](requisitos-funcionais.md#rf-06--proteção-contra-dupla-reserva-simultânea).

**Evidência:** execução repetível de testes com solicitações conflitantes simultâneas, registrando as entradas e os resultados e demonstrando que não há dupla confirmação incompatível.

**Critério quantitativo:** volume de solicitações concorrentes esperado não foi informado.

### RNF-03 — Rastreabilidade e auditabilidade

As mudanças relevantes devem ser rastreáveis por meio do histórico previsto em [RF-10](requisitos-funcionais.md#rf-10--histórico-auditável-de-mudanças).

**Evidência:** registros consultáveis que identifiquem ação, registro afetado, usuário responsável e data/hora, acompanhados de testes que verifiquem sua criação.

**Critério quantitativo:** prazo de retenção, proteção contra alteração dos registros e regras de acesso ao histórico ainda não foram definidos.

### RNF-04 — Responsividade e clareza das mensagens

As interfaces devem se adaptar a diferentes tamanhos de tela, e as mensagens de erro devem ser compreensíveis para orientar o usuário sobre o problema ocorrido.

**Evidência:** verificação dos fluxos principais em telas de diferentes dimensões e casos de erro representativos, confirmando que o conteúdo permanece utilizável e que as mensagens comunicam o problema sem expor detalhes internos desnecessários.

**Critério quantitativo:** dimensões de tela, navegadores e padrões de acessibilidade alvo não foram informados.

### RNF-05 — Documentação dos fluxos públicos

O sistema deve disponibilizar documentação atualizada da API ou, se não houver API pública, dos fluxos públicos do sistema.

**Evidência:** documentação revisável que corresponda à interface ou aos fluxos efetivamente disponibilizados e permita compreender suas entradas, resultados e erros observáveis.

**Critério quantitativo:** formato, local de publicação e convenções de versionamento da documentação ainda não foram definidos.

### RNF-06 — Evidência de desempenho

O desempenho deve ser avaliado com JMeter e medições reproduzíveis dos fluxos de consulta de disponibilidade e criação de reservas, alinhadas ao objetivo do projeto de apresentar evidências objetivas. A avaliação deve considerar todos estes aspectos: usuários concorrentes, duração, volume de dados, tempo de resposta e taxa de erro.

**Evidência:** plano e relatório JMeter que informem ambiente, conjunto de dados, carga aplicada, duração, tempos de resposta e taxa de erro, permitindo reproduzir e comparar a avaliação.

**Critério quantitativo:** os aspectos a medir estão definidos, mas os valores-alvo de usuários concorrentes, duração, volume de dados, tempo de resposta e taxa de erro ainda precisam ser acordados antes de declarar aprovação de desempenho.

### RNF-07 — Estratégia automatizada de qualidade e gates

O projeto deve atender à estratégia de qualidade definida para a entrega:

- Testes unitários com JUnit 5, incluindo testes parametrizados, além de testes de integração, API em caixa-preta e end-to-end.
- Banco de dados containerizado com Testcontainers para testes de integração de persistência e WireMock para integrações externas críticas.
- Teste automatizado de concorrência que comprove proteção contra dupla reserva.
- Evidência de ao menos uma funcionalidade nova desenvolvida com TDD ou BDD.
- GitHub Actions executado em pull requests, cobertura medida com JaCoCo e projeto integrado ao SonarCloud.
- Cobertura agregada do projeto de pelo menos 80% de linhas e 70% de branches. Exclusões do JaCoCo são permitidas, mas devem ser explícitas e documentadas.
- Na entrega final, zero bugs críticos e zero vulnerabilidades críticas.

**Evidência:** resultados dos testes e dos workflows de CI, relatório JaCoCo, análise do SonarCloud, evidência do teste concorrente e registro do ciclo TDD/BDD. O gate de zero itens críticos aplica-se à entrega final.

**Critérios pendentes:** a lista de exclusões do JaCoCo deve ser definida e justificada; o critério/fonte de classificação de criticidade para bugs e vulnerabilidades também deve ser registrado.

## Rastreabilidade

| Atributo ou entregável | Origem | Requisitos relacionados |
|---|---|---|
| Segurança por perfil | Requisito funcional obrigatório 1 | RNF-01, RF-01 |
| Integridade de reservas concorrentes | Requisitos funcionais obrigatórios 5 e 6 | RNF-02, RF-05, RF-06 |
| Histórico auditável | Requisito funcional obrigatório da lista complementar 3 | RNF-03, RF-10 |
| Interface responsiva e mensagens compreensíveis | Requisito funcional obrigatório da lista complementar 6 | RNF-04 |
| Documentação da API ou dos fluxos públicos | Requisito funcional obrigatório da lista complementar 7 | RNF-05 |
| Desempenho medido com JMeter | Estratégia obrigatória e objetivo do projeto | RNF-06 |
| Automação, cobertura, análise estática e gate final | Estratégia obrigatória e metas mensuráveis | RNF-07 |

## Decisões pendentes para avaliação

- Aprovar valores-alvo para todos os aspectos medidos com JMeter: usuários concorrentes, duração, volume de dados, tempo de resposta e taxa de erro.
- Confirmar requisitos de segurança adicionais, navegadores e padrões de acessibilidade aplicáveis.
- Definir política de retenção e proteção dos registros de auditoria.
- Definir formato, publicação e manutenção da documentação pública.
- Definir e justificar as exclusões específicas do JaCoCo e registrar a fonte de classificação de bugs e vulnerabilidades críticas.