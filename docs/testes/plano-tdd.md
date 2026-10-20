# Plano TDD — Organização de Recursos

## Objetivo e limites

Este documento inventaria regras e cenários de teste a partir das fontes do projeto. Na preparação original, não foram implementados testes nem código de produção. No ciclo RED, os casos abaixo foram reavaliados contra suas fontes; nenhum teste foi escrito porque o workspace não contém projeto Java/JUnit nem APIs de produção a testar. Casos marcados como pendentes também não podem receber expectativas inventadas: precisam de decisão registrada antes de virarem asserts de aceitação.

As fontes consultadas foram [requisitos funcionais](../requisitos-funcionais.md), [requisitos não funcionais](../requisitos-nao-funcionais.md), [matriz de rastreabilidade](../matriz-rastreabilidade.md), [arquitetura](../arquitetura.md), [persona Solicitante](../personas/persona-solicitante.md), [persona Responsável](../personas/persona-responsavel.md), [persona Administrador](../personas/persona-administrador.md) e [README](../../README.md).

**Estados neste plano:** `Definido` significa que a fonte fornece o resultado esperado; `Pendente` significa que a fonte não permite concluir um resultado de aceitação. As categorias “conflito” e “estado proibido” são marcadas como não aplicáveis somente quando a regra não define esse tipo de comportamento.

## Inventário de regras e casos

### RF-01 — Autenticação e autorização por perfil

**Origem:** `RF-01`; complementos em `RF-04`, `RF-07` e nas três personas. A matriz detalhada de permissões permanece pendente.

- **Caminho feliz — Definido:** usuário autenticado executa uma ação autorizada para seu perfil; a ação é permitida.
- **Limite — Definido:** Solicitante gerencia a própria reserva; Responsável decide solicitação de recurso restrito; Administrador mantém cadastros, usuários, permissões e indisponibilidades; cada ação descrita para o perfil é permitida.
- **Entrada inválida — Definido/Pendente:** credencial inválida ou ausência de autenticação não permite operação protegida. Formato, mecanismo de autenticação e sessão não estão definidos.
- **Conflito — Definido:** Solicitante tenta gerenciar reserva de terceiro ou perfil diferente de Responsável tenta aprovar recurso restrito; acesso negado conforme `RF-04`, `RF-07` e arquitetura.
- **Estado proibido — Pendente:** tentativa por perfil autorizado em estado de reserva não permitido; a matriz perfil × estado ainda não existe.

### RF-02 — Cadastro e consulta de recursos

**Origem:** `RF-02`; atividades e expectativas da persona Administrador.

- **Caminho feliz — Definido:** cadastrar e consultar uma sala, um professor e um material; cada recurso cadastrado pode ser localizado e consultado.
- **Limite — Pendente:** valores mínimos/máximos e campos obrigatórios por tipo não foram especificados.
- **Entrada inválida — Parcial:** cadastro incompleto deve ser impedido conforme expectativa da persona; quais campos são obrigatórios e a resposta observável precisam ser definidos.
- **Conflito — Pendente:** cadastro duplicado ou atributos conflitantes; não há regra de unicidade nem resolução documentada.
- **Estado proibido — Não aplicável:** a fonte não define ciclo de vida de cadastro de recurso.

### RF-03 — Pesquisa de recursos

**Origem:** `RF-03`; critérios reproduzidos na RTM.

- **Caminho feliz — Definido:** pesquisar por tipo, capacidade, localização, competência e disponibilidade, individualmente, usando atributos aplicáveis; resultados correspondem ao critério informado.
- **Limite — Pendente:** capacidade exatamente igual ao valor pesquisado, limites inclusivos e combinação de critérios dependem da semântica de cada filtro, ainda não documentada.
- **Entrada inválida — Pendente:** atributo incompatível com o tipo, valor malformado ou filtro desconhecido; a fonte não define rejeição versus ignorar filtro.
- **Conflito — Parcial:** combinação de filtros sem recurso correspondente deve retornar conjunto sem correspondências; operador AND/OR e interpretação de filtros combinados precisam ser confirmados.
- **Estado proibido — Pendente:** disponibilidade de recursos em estados específicos de reserva/manutenção está definida em `RF-05`/`RF-08`, mas estados e atributos pesquisáveis por tipo não estão mapeados.

### RF-04 — Gerenciamento de reservas

**Origem:** `RF-04`; persona Solicitante; limites de perfil explicitados na própria regra.

- **Caminho feliz — Definido:** Solicitante cria, altera e cancela uma reserva própria; cada operação aceita produz o estado resultante esperado para aquela operação.
- **Limite — Pendente:** quais estados permitem alteração ou cancelamento não foi definido.
- **Entrada inválida — Definido:** tentar criar/alterar com término igual ou anterior ao início deve ser rejeitado por `RF-13`.
- **Conflito — Definido:** alteração ou criação que sobreponha recurso/professor ou ocorra durante manutenção não deve confirmar reserva incompatível (`RF-05`, `RF-08`).
- **Estado proibido — Pendente:** cancelamento em `APROVADA`, `EM_USO` ou demais estados; `RF-15` proíbe apagar em `EM_USO`, mas não define cancelamento nesse estado.
- **Autorização — Definido:** Solicitante não gerencia reserva alheia; não presumir que Administrador possa cancelar reserva de terceiro.

### RF-05 — Detecção de sobreposição de horários

**Origem:** `RF-05`; definição de sobreposição no glossário da arquitetura; criticidade na RTM.

- **Caminho feliz — Definido:** reservas sem sobreposição para o mesmo recurso e professor não são sinalizadas como conflito.
- **Limite — Pendente:** períodos que apenas encostam no início/fim; a regra funcional registra explicitamente essa decisão como pendente.
- **Entrada inválida — Definido:** intervalo com término igual ou anterior ao início é rejeitado por `RF-13`, não tratado como reserva válida.
- **Conflito — Definido:** sobreposição parcial, total ou contida para a mesma sala, material ou professor é identificada; uma reserva conflitante não deve ser aceita.
- **Estado proibido — Pendente:** quais estados de reserva ocupam agenda para fins de conflito não estão discriminados.

### RF-06 — Proteção contra dupla reserva simultânea

**Origem:** `RF-06`, `RNF-02`; cenário de concorrência e Testcontainers na RTM.

- **Caminho feliz — Definido:** solicitações simultâneas compatíveis, sem disputa por recurso/professor e horário, não são rejeitadas por conflito.
- **Limite — Definido/Pendente:** duas solicitações válidas simultâneas para o mesmo período e recurso/professor confirmam no máximo uma; volume de concorrência esperado não foi definido.
- **Entrada inválida — Definido:** solicitação com intervalo inválido é rejeitada conforme `RF-13`, mesmo sob concorrência.
- **Conflito — Definido:** executar repetidamente requisições incompatíveis concorrentes; no máximo uma reserva incompatível é confirmada e a não atendida recebe retorno compreensível.
- **Estado proibido — Pendente:** estados considerados ocupantes e transições durante disputa não estão completamente definidos.

### RF-07 — Aprovação de recursos restritos

**Origem:** `RF-07`; persona Responsável; criticidade na RTM.

- **Caminho feliz — Definido:** solicitação restrita recebe decisão de perfil Responsável; decisão é registrada e a solicitação não é tratada como aprovada antes dela.
- **Limite — Pendente:** conjunto de recursos classificados como restritos não está definido.
- **Entrada inválida — Pendente:** decisão sem dados obrigatórios ou justificativa; dados exigidos não foram especificados.
- **Conflito — Definido:** solicitante ou Administrador tenta aprovar solicitação restrita; operação negada, pois a aprovação exige perfil Responsável.
- **Estado proibido — Parcial:** antes da decisão, a reserva não pode estar aprovada. Demais transições posteriores à decisão dependem da matriz de estados pendente.

### RF-08 — Bloqueio de recursos em manutenção

**Origem:** `RF-08`; atividades e expectativas da persona Administrador; criticidade na RTM.

- **Caminho feliz — Definido:** Administrador registra período de manutenção; durante esse período o recurso aparece indisponível e não pode ser reservado.
- **Limite — Pendente:** inclusão/exclusão dos instantes inicial e final do bloqueio não está definida.
- **Entrada inválida — Pendente:** período invertido/vazio deve ser tratado com semântica equivalente a intervalo inválido, mas a regra não especifica validação de período de manutenção.
- **Conflito — Pendente:** manutenção criada sobre reserva existente e sobreposição entre bloqueios; a política de impacto/resolução não foi aprovada.
- **Estado proibido — Parcial:** reserva durante o período de manutenção é proibida; comportamento para reserva fora do período é o fluxo normal de disponibilidade.

### RF-09 — Retirada e devolução de materiais

**Origem:** `RF-09`; atividades e necessidades da persona Responsável.

- **Caminho feliz — Definido:** registrar retirada e devolução de material/equipamento; consultar o estado da movimentação registrado.
- **Limite — Pendente:** quantidades, prazos e significado de devolução parcial não foram definidos.
- **Entrada inválida — Pendente:** devolução sem retirada, material inexistente ou quantidade inválida; regras de rejeição não especificadas.
- **Conflito — Pendente:** duas retiradas concorrentes do mesmo item/quantidade e movimentação vinculada a reserva cancelada; estoque e política de conflito não documentados.
- **Estado proibido — Pendente:** estados da movimentação e transições permitidas não foram nomeados.

### RF-10 — Histórico auditável de mudanças

**Origem:** `RF-10`, `RNF-03`; evidência e criticidade na RTM.

- **Caminho feliz — Definido:** mudança em registro gerenciado e mudança de estado de reserva geram registro consultável com ação, objeto afetado, usuário responsável e data/hora.
- **Limite — Parcial:** toda mudança de estado deve gerar auditoria; retenção, precisão temporal e limite de consulta não estão definidos.
- **Entrada inválida — Pendente:** conteúdo inválido de evento e comportamento se auditoria falhar não estão especificados.
- **Conflito — Pendente:** comando negado, conflito de reserva ou falha parcial pode ou não gerar evento; a fonte não define auditoria de tentativas sem mudança.
- **Estado proibido — Pendente:** proteção contra alteração/exclusão e regras de acesso ao histórico estão em aberto.

### RF-11 — Notificações

**Origem:** `RF-11`; alternativa simulada/API externa; RTM.

- **Caminho feliz — Definido:** criação, alteração e cancelamento de reserva produzem, cada um, envio ou simulação observável de notificação.
- **Limite — Pendente:** ordem, destinatários e momento de notificar pedido pendente versus aprovado não estão definidos.
- **Entrada inválida — Pendente:** dados de notificação malformados e falha do provedor não têm resultado de aceitação definido.
- **Conflito — Pendente:** falha, retry e duplicidade de entrega; ADR-004 é proposta, não requisito aprovado.
- **Estado proibido — Pendente:** notificar tentativa rejeitada/conflitante ou transição não permitida; a regra não estabelece quais eventos sem sucesso notificam.

### RF-12 — Relatórios de utilização e conflitos evitados

**Origem:** `RF-12`; RTM; lacunas apontadas na arquitetura.

- **Caminho feliz — Definido:** relatório mensal apresenta utilização por recurso, carga horária alocada e conflitos evitados para dados conhecidos.
- **Limite — Pendente:** fuso/fechamento do mês, limites do período e inclusão de reservas canceladas não estão definidos.
- **Entrada inválida — Pendente:** mês/ano inválido ou período sem dados; resposta não especificada.
- **Conflito — Pendente:** fórmula e semântica de “conflito evitado” não estão definidas; não é possível fixar totais esperados.
- **Estado proibido — Não aplicável:** a regra define conteúdo de relatório, não transições de estado.

### RF-13 — Validação do intervalo da reserva

**Origem:** `RF-13`; teste parametrizado de início/término previsto na RTM.

- **Caminho feliz — Definido:** término posterior ao início é aceito.
- **Limite — Definido:** término igual ao início é rejeitado.
- **Entrada inválida — Definido:** término anterior ao início é rejeitado. Formatos de data/hora malformados dependem do contrato de entrada, não documentado.
- **Conflito — Não aplicável:** esta regra valida intervalo, não agenda concorrente; cruzar com `RF-05`/`RF-06` para combinação.
- **Estado proibido — Não aplicável:** a regra não define ciclo de vida da reserva.

### RF-14 — Ciclo de vida da reserva

**Origem:** `RF-14`; fluxo e estados na RTM; lacuna de matriz de transições registrada na arquitetura.

- **Caminho feliz — Definido:** exercitar `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`; cada transição do fluxo principal resulta no estado seguinte.
- **Limite — Parcial:** representar `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`; evento disparador/perfil de cada um ainda não está definido.
- **Entrada inválida — Pendente:** valor de estado desconhecido ou payload de transição inválido; resposta não especificada.
- **Conflito — Pendente:** duas decisões/transições concorrentes sobre a mesma reserva; política não definida.
- **Estado proibido — Pendente:** transições fora do fluxo principal e da matriz alternativa (por exemplo, concluir antes de iniciar) precisam de matriz aprovada antes de afirmar quais são proibidas.

### RF-15 — Preservação de reservas iniciadas

**Origem:** `RF-15`; criticidade e evidência na RTM.

- **Caminho feliz — Definido:** reserva em `EM_USO` permanece consultável e disponível para auditoria.
- **Limite — Definido:** imediatamente após entrar em `EM_USO`, a tentativa de apagar continua negada.
- **Entrada inválida — Pendente:** identificador inexistente/malformado não tem resposta documentada.
- **Conflito — Não aplicável:** exclusão em `EM_USO` é proibida independentemente de concorrência; consistência concorrente da exclusão não foi especificada.
- **Estado proibido — Definido/Pendente:** apagar em `EM_USO` é negado e o registro permanece; permissão de apagar em outros estados não é definida.

### RNF-01 — Segurança e controle de acesso

**Origem:** `RNF-01`, referenciando `RF-01`; evidência de autorização por perfil.

- **Caminho feliz — Definido:** ações autorizadas são permitidas e ações sem permissão são negadas nos perfis configurados.
- **Limite — Pendente:** política de sessão, identidade e controles adicionais não foram definidos.
- **Entrada inválida — Pendente:** ameaças/formatos além das tentativas sem permissão dependem de requisitos de segurança ainda não aprovados.
- **Conflito — Definido:** nenhuma ação deve ampliar privilégios em conflito com o perfil; usar os cenários de `RF-01`/`RF-07`.
- **Estado proibido — Pendente:** autorização por estado depende da matriz de `RF-14`.

### RNF-02 — Integridade sob concorrência

**Origem:** `RNF-02`, `RF-05`, `RF-06`; execução repetível de concorrência prevista na RTM.

- **Caminho feliz — Definido:** resultados concorrentes sem incompatibilidade preservam as reservas válidas.
- **Limite — Pendente:** quantidade/carga concorrente de referência não informada.
- **Entrada inválida — Definido:** entradas com intervalo inválido continuam rejeitadas sob execução simultânea (`RF-13`).
- **Conflito — Definido:** disputas pelo mesmo recurso/professor e período não produzem mais de uma confirmação incompatível; registrar entradas e resultados.
- **Estado proibido — Parcial:** estado final inconsistente com a regra de não dupla confirmação é falha; estados elegíveis para disputa dependem de `RF-14`.

### RNF-03 — Rastreabilidade e auditabilidade

**Origem:** `RNF-03`, `RF-10`.

- **Caminho feliz — Definido:** cada transição de estado e mudança relevante apresenta ação, registro, autor e data/hora consultáveis.
- **Limite — Pendente:** prazo de retenção, proteção contra alteração e escala do histórico não definidos.
- **Entrada inválida — Pendente:** falha de gravação/auditoria e critérios de integridade do registro não definidos.
- **Conflito — Pendente:** consistência atômica entre operação e auditoria é proposta no ADR-003, não requisito aprovado.
- **Estado proibido — Pendente:** alteração ou acesso indevido ao histórico dependem de política ainda não definida.

### RNF-04 — Responsividade e clareza das mensagens

**Origem:** `RNF-04`; evidência de E2E e responsividade na RTM.

- **Caminho feliz — Definido:** fluxos principais permanecem utilizáveis em diferentes tamanhos de tela.
- **Limite — Pendente:** viewports, navegadores e padrões de acessibilidade alvo não foram informados.
- **Entrada inválida — Definido:** erros representativos mostram mensagem compreensível sem expor detalhes internos desnecessários.
- **Conflito — Não aplicável:** não define resolução de conflitos de negócio; mensagens de conflitos devem ser cobertas junto a `RF-05`/`RF-06`.
- **Estado proibido — Não aplicável:** requisito de apresentação, sem transições de domínio.

### RNF-05 — Documentação dos fluxos públicos

**Origem:** `RNF-05`; evidência de revisão documental na RTM.

- **Caminho feliz — Definido:** documentação atual corresponde à API pública ou, sem API pública, aos fluxos públicos efetivamente disponibilizados e descreve entradas, resultados e erros observáveis.
- **Limite — Pendente:** formato, local de publicação e versionamento não definidos.
- **Entrada inválida — Definido:** discrepância entre documentação e comportamento público reprova a conferência.
- **Conflito — Não aplicável:** não define conflitos de execução; inconsistências entre contratos publicados e observados são falha documental.
- **Estado proibido — Não aplicável:** requisito documental, sem estados de domínio.

### RNF-06 — Evidência de desempenho

**Origem:** `RNF-06`; relatório JMeter e dimensões obrigatórias na RTM.

- **Caminho feliz — Definido:** executar medições reproduzíveis de consulta de disponibilidade e criação de reserva, informando concorrência, duração, volume, tempo de resposta e taxa de erro.
- **Limite — Pendente:** valores-alvo para cada dimensão não foram aprovados; não declarar aprovado/reprovado por limiar presumido.
- **Entrada inválida — Definido:** relatório sem ambiente, conjunto de dados, carga ou medidas requeridas é evidência incompleta.
- **Conflito — Não aplicável:** não define política de conflito; incluir o comportamento de conflito nos fluxos medidos somente quando o perfil de carga for aprovado.
- **Estado proibido — Não aplicável:** requisito de desempenho, sem estados de domínio.

### RNF-07 — Estratégia automatizada de qualidade e gates

**Origem:** `RNF-07`; evidências e metas descritas na RTM.

- **Caminho feliz — Definido:** verificar testes JUnit 5 (incluindo parametrizados), integração/API/E2E, Testcontainers para persistência, WireMock para integração externa crítica, teste automatizado de concorrência, evidência de ao menos uma funcionalidade nova em TDD/BDD, GitHub Actions em PR, JaCoCo e SonarCloud.
- **Limite — Definido:** cobertura agregada de exatamente 80% de linhas e 70% de branches satisfaz as metas; valores abaixo de qualquer meta não satisfazem o gate. Exclusões só contam se explícitas e documentadas.
- **Entrada inválida — Definido/Pendente:** falha de testes/workflow ou relatório de cobertura ausente não comprova o gate; exclusões permitidas precisam de lista e justificativa ainda pendentes.
- **Conflito — Não aplicável:** conflito de agenda é coberto por `RF-05`/`RF-06` e pelo teste concorrente obrigatório.
- **Estado proibido — Definido/Pendente:** entrega final com zero bugs críticos e zero vulnerabilidades críticas satisfaz esse critério; qualquer item crítico reprova. Fonte de classificação de criticidade precisa ser registrada.
- **Observação:** cobertura não substitui evidência comportamental; a fonte exige os dois tipos de evidência.

### Restrição do job diário — sem identificador RF/RNF

**Origem:** restrição registrada em [arquitetura, seção 1](../arquitetura.md): executar diariamente ao final do dia e treinar/retreinar usando somente dados novos. A própria arquitetura informa que a regra não consta nos requisitos/RTM e precisa ser formalizada. Watermark, exclusividade, retries, manifesto e promoção presentes nos ADRs/diagramas são propostas, não regras aprovadas.

- **Caminho feliz — Parcial:** disparo diário ao final do dia treina/retreina usando exclusivamente o conjunto classificado como novo; a definição operacional de “novo” e fuso/horário exatos estão pendentes, então o teste de aceitação está bloqueado até aprovação.
- **Limite — Pendente:** execução no limite do dia, lote vazio, dados atrasados e mudança de fuso não têm critérios definidos.
- **Entrada inválida — Pendente:** dados inválidos/degradados e política de quarentena ou bloqueio do job são propostas de arquitetura, não critérios aprovados.
- **Conflito — Pendente:** sobreposição/reexecução diária e comportamento em falha não foram formalizados; não adotar lock/retry de ADR-006 como assert obrigatório.
- **Estado proibido — Parcial:** treinar com dados que não atendam à definição aprovada de “novos” contrariaria a restrição; sem fonte e definição de novidade não há fixture de aceitação verificável.

## Ambiguidades, lacunas e perguntas para desbloqueio

1. Qual é a matriz completa de permissões por perfil, operação e estado? Quem inicia, aprova, rejeita, cancela, conclui e registra `NÃO_COMPARECEU`?
2. Quais transições são permitidas entre estados alternativos e quais transições são proibidas? Em quais estados o Solicitante pode alterar/cancelar?
3. Quais atributos, operadores e combinações de filtros se aplicam a cada tipo de recurso? Capacidade é igualdade ou mínimo requerido?
4. Quais recursos são restritos? Qual o estado exato da reserva antes/depois de aprovação ou rejeição?
5. Intervalos são fechados ou semiabertos? Reservas/manutenções que apenas encostam no limite conflitam?
6. Quais campos são obrigatórios por recurso/reserva e quais validações de unicidade devem existir?
7. Ao registrar manutenção sobre reserva existente, bloqueia-se o cadastro, cancela-se a reserva ou há outro fluxo? Como tratar intervalos inválidos de manutenção?
8. Quais estados, quantidades, prazos e transições existem para retirada/devolução? Há controle de estoque e política para devolução sem retirada?
9. Quais mudanças são auditadas além de transições? Como tratar tentativas negadas e falha de persistência? Qual retenção, proteção e acesso ao histórico?
10. Quem recebe notificações, em qual momento (solicitada/aprovada), e qual comportamento esperado em falha, retry e duplicidade?
11. Quais fórmulas definem utilização, carga horária e conflito evitado? Qual fuso fecha o mês e como reservas canceladas entram no cálculo?
12. Quais viewports, navegadores e padrões de acessibilidade são alvo? Qual formato/local/versionamento da documentação pública?
13. Quais metas e perfil de carga JMeter devem decidir aprovação? Quais exclusões JaCoCo são justificáveis e qual fonte define criticidade?
14. Para o job, qual fonte autorizada, critério de “dado novo”, horário/fuso e resultado esperado? Como tratar lote vazio/inválido, dados tardios, falhas e reexecução?
15. Qual é a finalidade/consumidor do modelo, critério de sucesso e autoridade para promoção? A arquitetura registra que métricas, baseline e promoção não estão aprovados.

## Artefatos não encontrados

- Não foi encontrado documento separado de regras de negócio, restrições ou decisões anteriores em `docs/`; a busca encontrou apenas os documentos listados no escopo acima.
- Não foram encontrados arquivos ADR do produto. A seção 16 da arquitetura contém ADRs **propostos**, explicitamente não aceitos.
- Não foram encontrados código de produção nem testes do produto na busca por arquivos de implementação/teste fora do framework `.aiox-core/`; também não há diretório de testes do produto no workspace inspecionado.
- `docs/testes/` não existia antes deste plano.

## Matriz de cobertura — ciclo RED

**Resultado da execução:** `mvn test` não iniciou testes: Maven encerrou com `BUILD FAILURE` porque não há `pom.xml` no diretório raiz. A busca também não encontrou arquivos `.java`, `build.gradle*`, wrappers Maven/Gradle ou diretórios de teste do produto. Portanto, não existem nomes de testes JUnit reais nem resultados RED para registrar. A coluna “Teste” usa `—` para deixar explícita a ausência, não para sugerir um nome futuro.

**Atualização do ambiente (2026-10-20):** foi criado o scaffold Maven com JUnit 5, Surefire e Failsafe, e `./mvnw -B verify` concluiu com `BUILD SUCCESS`. A saída informa “No tests to run” em ambas as suítes; o resultado valida somente o lifecycle, não comportamento de negócio. Os 116 casos continuam sem teste executável.

**Status:** `BLOQUEADO — sem teste de comportamento` para casos com resultado definido; `BLOQUEADO — requisito pendente e sem teste de comportamento` quando a própria fonte não define o resultado; `N/A documentado` quando o plano declara que a categoria não define comportamento testável independente. Uma regra só será considerada coberta quando houver teste executável e evidência do resultado esperado.

| Regra | Caso do plano | Teste JUnit 5 | Status |
|---|---|---|---|
| RF-01 | Caminho feliz: usuário autenticado executa ação autorizada | — | BLOQUEADO — sem teste de comportamento |
| RF-01 | Limite: Solicitante próprio; Responsável aprova restrita; Administrador mantém cadastros/permissões/indisponibilidades | — | BLOQUEADO — matriz de ações incompleta e sem teste de comportamento |
| RF-01 | Entrada inválida: credencial inválida ou ausência de autenticação nega ação protegida | — | BLOQUEADO — sem teste de comportamento |
| RF-01 | Conflito: Solicitante em reserva alheia ou perfil não Responsável aprovando restrita é negado | — | BLOQUEADO — sem teste de comportamento |
| RF-01 | Estado proibido: perfil × estado não permitido | — | BLOQUEADO — transições/permissões pendentes e sem teste de comportamento |
| RF-02 | Caminho feliz: cadastrar e consultar sala, professor e material | — | BLOQUEADO — sem teste de comportamento |
| RF-02 | Limite: valores mínimos/máximos e campos obrigatórios por tipo | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RF-02 | Entrada inválida: cadastro incompleto é impedido | — | BLOQUEADO — campos/resposta pendentes e sem teste de comportamento |
| RF-02 | Conflito: recurso duplicado ou atributos conflitantes | — | BLOQUEADO — regra de unicidade pendente e sem teste de comportamento |
| RF-02 | Estado proibido: ciclo de vida do cadastro | — | N/A documentado — fonte não define estados |
| RF-03 | Caminho feliz: pesquisa por tipo, capacidade, localização, competência e disponibilidade | — | BLOQUEADO — sem teste de comportamento |
| RF-03 | Limite: capacidade igual, inclusividade e combinação de critérios | — | BLOQUEADO — semântica pendente e sem teste de comportamento |
| RF-03 | Entrada inválida: atributo incompatível, valor malformado ou filtro desconhecido | — | BLOQUEADO — rejeitar/ignorar pendente e sem teste de comportamento |
| RF-03 | Conflito: combinação sem correspondências retorna conjunto vazio | — | BLOQUEADO — AND/OR pendente e sem teste de comportamento |
| RF-03 | Estado proibido: disponibilidade por estados de reserva/manutenção | — | BLOQUEADO — mapeamento de estados/atributos pendente e sem teste de comportamento |
| RF-04 | Caminho feliz: Solicitante cria, altera e cancela reserva própria | — | BLOQUEADO — sem teste de comportamento |
| RF-04 | Limite: estados nos quais alteração/cancelamento são permitidos | — | BLOQUEADO — matriz de estados pendente e sem teste de comportamento |
| RF-04 | Entrada inválida: término igual/anterior ao início rejeitado por RF-13 | — | BLOQUEADO — sem teste de comportamento |
| RF-04 | Conflito: criação/alteração sobreposta ou durante manutenção não confirma | — | BLOQUEADO — sem teste de comportamento |
| RF-04 | Estado proibido: cancelar em APROVADA, EM_USO e demais estados | — | BLOQUEADO — regra pendente e sem teste de comportamento |
| RF-04 | Autorização: Solicitante não gerencia reserva alheia; poder administrativo não presumido | — | BLOQUEADO — sem teste de comportamento |
| RF-05 | Caminho feliz: períodos sem sobreposição não são conflito | — | BLOQUEADO — sem teste de comportamento |
| RF-05 | Limite: períodos que apenas encostam nos limites | — | BLOQUEADO — regra pendente e sem teste de comportamento |
| RF-05 | Entrada inválida: intervalo igual/invertido rejeitado por RF-13 | — | BLOQUEADO — sem teste de comportamento |
| RF-05 | Conflito: sobreposição parcial, total ou contida de sala/material/professor identificada | — | BLOQUEADO — sem teste de comportamento |
| RF-05 | Estado proibido: estados que ocupam a agenda | — | BLOQUEADO — estados não discriminados e sem teste de comportamento |
| RF-06 | Caminho feliz: solicitações concorrentes compatíveis preservadas | — | BLOQUEADO — sem teste de comportamento |
| RF-06 | Limite: duas solicitações incompatíveis simultâneas confirmam no máximo uma; volume esperado | — | BLOQUEADO — volume pendente e sem teste de comportamento |
| RF-06 | Entrada inválida: intervalo inválido continua rejeitado sob concorrência | — | BLOQUEADO — sem teste de comportamento |
| RF-06 | Conflito: no máximo uma confirmação incompatível e retorno compreensível à não atendida | — | BLOQUEADO — sem teste de comportamento |
| RF-06 | Estado proibido: estados elegíveis para disputa | — | BLOQUEADO — estados não definidos e sem teste de comportamento |
| RF-07 | Caminho feliz: Responsável decide solicitação restrita, decisão registrada e não aprovada antes | — | BLOQUEADO — sem teste de comportamento |
| RF-07 | Limite: conjunto de recursos restritos | — | BLOQUEADO — classificação pendente e sem teste de comportamento |
| RF-07 | Entrada inválida: decisão sem dados/justificativa exigidos | — | BLOQUEADO — campos exigidos pendentes e sem teste de comportamento |
| RF-07 | Conflito: Solicitante/Administrador tentando aprovar é negado | — | BLOQUEADO — sem teste de comportamento |
| RF-07 | Estado proibido: reserva restrita aprovada antes da decisão | — | BLOQUEADO — sem teste de comportamento |
| RF-08 | Caminho feliz: manutenção torna recurso indisponível e não reservável no período | — | BLOQUEADO — sem teste de comportamento |
| RF-08 | Limite: inclusão/exclusão dos instantes inicial/final | — | BLOQUEADO — intervalo temporal pendente e sem teste de comportamento |
| RF-08 | Entrada inválida: período de manutenção invertido/vazio | — | BLOQUEADO — regra de validação pendente e sem teste de comportamento |
| RF-08 | Conflito: manutenção sobre reserva existente ou sobreposição de bloqueios | — | BLOQUEADO — política pendente e sem teste de comportamento |
| RF-08 | Estado proibido: reservar durante manutenção | — | BLOQUEADO — sem teste de comportamento |
| RF-09 | Caminho feliz: registrar e consultar retirada/devolução | — | BLOQUEADO — sem teste de comportamento |
| RF-09 | Limite: quantidades, prazos e devolução parcial | — | BLOQUEADO — semântica pendente e sem teste de comportamento |
| RF-09 | Entrada inválida: devolver sem retirada, material inexistente ou quantidade inválida | — | BLOQUEADO — regras pendentes e sem teste de comportamento |
| RF-09 | Conflito: retiradas concorrentes/estoque e reserva cancelada | — | BLOQUEADO — estoque/política pendentes e sem teste de comportamento |
| RF-09 | Estado proibido: transições da movimentação | — | BLOQUEADO — estados não nomeados e sem teste de comportamento |
| RF-10 | Caminho feliz: mudança auditada com ação, objeto, autor e data/hora consultáveis | — | BLOQUEADO — sem teste de comportamento |
| RF-10 | Limite: toda transição auditada; retenção/precisão/consulta | — | BLOQUEADO — critérios parciais e sem teste de comportamento |
| RF-10 | Entrada inválida: evento inválido ou falha de auditoria | — | BLOQUEADO — comportamento pendente e sem teste de comportamento |
| RF-10 | Conflito: comando negado/conflito/falha parcial e geração de auditoria | — | BLOQUEADO — comportamento pendente e sem teste de comportamento |
| RF-10 | Estado proibido: alteração/exclusão/acesso indevido ao histórico | — | BLOQUEADO — política pendente e sem teste de comportamento |
| RF-11 | Caminho feliz: notificações observáveis para criação, alteração e cancelamento | — | BLOQUEADO — sem teste de comportamento |
| RF-11 | Limite: ordem, destinatários e notificação de pendente/aprovada | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RF-11 | Entrada inválida: payload inválido/falha do provedor | — | BLOQUEADO — resultado pendente e sem teste de comportamento |
| RF-11 | Conflito: falha, retry e duplicidade | — | BLOQUEADO — ADR-004 é proposta; sem teste de comportamento |
| RF-11 | Estado proibido: notificar tentativa rejeitada/conflitante | — | BLOQUEADO — eventos sem sucesso não definidos e sem teste de comportamento |
| RF-12 | Caminho feliz: relatório mensal com utilização, horas e conflitos evitados | — | BLOQUEADO — sem teste de comportamento |
| RF-12 | Limite: fuso/fechamento do mês e reservas canceladas | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RF-12 | Entrada inválida: mês/ano inválido ou período sem dados | — | BLOQUEADO — resposta pendente e sem teste de comportamento |
| RF-12 | Conflito: cálculo dos conflitos evitados | — | BLOQUEADO — fórmula pendente e sem teste de comportamento |
| RF-12 | Estado proibido: transição de domínio pelo relatório | — | N/A documentado — requisito define conteúdo, não transição |
| RF-13 | Caminho feliz: término posterior ao início aceito | — | BLOQUEADO — sem teste de comportamento |
| RF-13 | Limite: término igual ao início rejeitado | — | BLOQUEADO — sem teste de comportamento |
| RF-13 | Entrada inválida: término anterior rejeitado; formato malformado | — | BLOQUEADO — formato não definido e sem teste de comportamento |
| RF-13 | Conflito: conflito de agenda, coberto por RF-05/RF-06 | — | N/A documentado — validar pela regra referenciada |
| RF-13 | Estado proibido: ciclo de vida da reserva | — | N/A documentado — requisito não define estados |
| RF-14 | Caminho feliz: SOLICITADA → APROVADA → EM_USO → CONCLUÍDA | — | BLOQUEADO — sem teste de comportamento |
| RF-14 | Limite: representar REJEITADA, CANCELADA e NÃO_COMPARECEU | — | BLOQUEADO — evento/perfil pendentes e sem teste de comportamento |
| RF-14 | Entrada inválida: estado desconhecido/payload inválido | — | BLOQUEADO — resposta pendente e sem teste de comportamento |
| RF-14 | Conflito: decisões/transições concorrentes | — | BLOQUEADO — política pendente e sem teste de comportamento |
| RF-14 | Estado proibido: transição fora da matriz aprovada | — | BLOQUEADO — matriz pendente e sem teste de comportamento |
| RF-15 | Caminho feliz: reserva EM_USO continua consultável/auditável | — | BLOQUEADO — sem teste de comportamento |
| RF-15 | Limite: apagar imediatamente após entrar em EM_USO é negado | — | BLOQUEADO — sem teste de comportamento |
| RF-15 | Entrada inválida: identificador inexistente/malformado | — | BLOQUEADO — resposta pendente e sem teste de comportamento |
| RF-15 | Conflito: exclusão concorrente | — | BLOQUEADO — concorrência não especificada e sem teste de comportamento |
| RF-15 | Estado proibido: apagar EM_USO negado e registro preservado | — | BLOQUEADO — sem teste de comportamento |
| RNF-01 | Caminho feliz: autorizações permitidas/negadas por perfil | — | BLOQUEADO — sem teste de comportamento |
| RNF-01 | Limite: identidade, sessão e controles adicionais | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RNF-01 | Entrada inválida: tentativas não autorizadas | — | BLOQUEADO — requisitos adicionais pendentes e sem teste de comportamento |
| RNF-01 | Conflito: ação não amplia privilégio | — | BLOQUEADO — sem teste de comportamento |
| RNF-01 | Estado proibido: autorização por estado | — | BLOQUEADO — depende da matriz RF-14 e sem teste de comportamento |
| RNF-02 | Caminho feliz: concorrência compatível preserva reservas válidas | — | BLOQUEADO — sem teste de comportamento |
| RNF-02 | Limite: volume concorrente esperado | — | BLOQUEADO — volume pendente e sem teste de comportamento |
| RNF-02 | Entrada inválida: intervalo inválido sob concorrência | — | BLOQUEADO — sem teste de comportamento |
| RNF-02 | Conflito: disputa não confirma dupla reserva; entradas/resultados registrados | — | BLOQUEADO — sem teste de comportamento |
| RNF-02 | Estado proibido: estado final com dupla confirmação incompatível | — | BLOQUEADO — estados elegíveis pendentes e sem teste de comportamento |
| RNF-03 | Caminho feliz: mudança/transição com ação, objeto, autor e data/hora | — | BLOQUEADO — sem teste de comportamento |
| RNF-03 | Limite: retenção, proteção e escala do histórico | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RNF-03 | Entrada inválida: falha de gravação/integridade do evento | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| RNF-03 | Conflito: atomicidade entre operação e auditoria | — | BLOQUEADO — ADR-003 é proposta; sem teste de comportamento |
| RNF-03 | Estado proibido: alteração/acesso indevido ao histórico | — | BLOQUEADO — política pendente e sem teste de comportamento |
| RNF-04 | Caminho feliz: fluxos principais utilizáveis em telas distintas | — | BLOQUEADO — sem aplicação/teste E2E |
| RNF-04 | Limite: viewports, navegadores e acessibilidade | — | BLOQUEADO — alvos pendentes e sem aplicação |
| RNF-04 | Entrada inválida: erro compreensível sem detalhe interno | — | BLOQUEADO — sem aplicação/teste E2E |
| RNF-04 | Conflito: mensagens de conflito RF-05/RF-06 | — | N/A documentado — comportamento coberto pelas regras referenciadas |
| RNF-04 | Estado proibido: transição de domínio | — | N/A documentado — requisito de apresentação |
| RNF-05 | Caminho feliz: documento corresponde ao contrato/fluxo público e descreve entradas/resultados/erros | — | BLOQUEADO — sem aplicação/contrato público |
| RNF-05 | Limite: formato, publicação e versionamento | — | BLOQUEADO — critérios pendentes e sem aplicação |
| RNF-05 | Entrada inválida: divergência documental reprova conferência | — | BLOQUEADO — sem aplicação/contrato público |
| RNF-05 | Conflito: inconsistência entre contrato e comportamento | — | N/A documentado — falha de conformidade documental, sem conflito de domínio |
| RNF-05 | Estado proibido: transição de domínio | — | N/A documentado — requisito documental |
| RNF-06 | Caminho feliz: relatório JMeter reproduzível com todas as dimensões listadas | — | BLOQUEADO — sem aplicação/plano de carga |
| RNF-06 | Limite: valores-alvo de carga e desempenho | — | BLOQUEADO — metas pendentes e sem aplicação |
| RNF-06 | Entrada inválida: relatório sem ambiente/dados/carga/medidas | — | BLOQUEADO — sem execução JMeter |
| RNF-06 | Conflito: política de conflito nos fluxos medidos | — | N/A documentado — regra de conflito está em RF-05/RF-06 |
| RNF-06 | Estado proibido: transição de domínio | — | N/A documentado — requisito de desempenho |
| RNF-07 | Caminho feliz: ferramentas, níveis de teste, CI, JaCoCo, SonarCloud e evidência TDD/BDD | — | BLOQUEADO — suíte/projeto/CI ausentes |
| RNF-07 | Limite: exatamente 80% linhas e 70% branches satisfaz; abaixo não satisfaz | — | BLOQUEADO — sem build/relatório JaCoCo |
| RNF-07 | Entrada inválida: falha de suíte/workflow ou cobertura ausente não comprova gate | — | BLOQUEADO — sem build/workflow |
| RNF-07 | Conflito: dupla reserva coberta pelo teste obrigatório RF-05/RF-06 | — | N/A documentado — comportamento exercitado nas regras referenciadas |
| RNF-07 | Estado proibido: qualquer bug/vulnerabilidade crítica aberta reprova entrega final | — | BLOQUEADO — fonte de severidade pendente e sem SonarCloud |
| Job diário (sem ID) | Caminho feliz: execução diária ao final do dia só usa dados novos | — | BLOQUEADO — critério “novo”, fuso e sem teste de comportamento |
| Job diário (sem ID) | Limite: fronteira do dia, lote vazio, dados tardios, mudança de fuso | — | BLOQUEADO — critérios pendentes e sem teste de comportamento |
| Job diário (sem ID) | Entrada inválida: lote inválido/degradado | — | BLOQUEADO — política proposta, não aprovada, e sem teste de comportamento |
| Job diário (sem ID) | Conflito: sobreposição/reexecução/falha | — | BLOQUEADO — regra formal pendente e sem teste de comportamento |
| Job diário (sem ID) | Estado proibido: treinar com registro não classificado como novo | — | BLOQUEADO — definição de novidade pendente e sem teste de comportamento |

**Regras sem teste executável:** todas — `RF-01` a `RF-15`, `RNF-01` a `RNF-07` e a restrição do job diário sem ID. Não há teste novo que passe ou falhe; portanto, não foi possível confirmar falha pelo motivo esperado nem investigar testes preexistentes que já passem.

**Impedimento para sair do RED:** ainda faltam código/contratos do produto e testes de comportamento, além das decisões pendentes listadas acima. Requisitos de API, E2E, JMeter, cobertura e análise estática precisam dos respectivos alvos/ferramentas; não podem ser substituídos por testes unitários JUnit inventados. Com esses materiais, cada linha bloqueada deve receber ao menos um teste real, ser executada e ter seu status atualizado nesta matriz.

## Ordem sugerida para o ciclo TDD

Começar pelos requisitos críticos da RTM: `RF-13` (intervalo), `RF-05`/`RF-06` (conflito e concorrência), `RF-07`/`RF-08` (restrição e manutenção), `RF-10` (auditoria), `RF-14`/`RF-15` (estados e preservação), e `RNF-02`/`RNF-03` (integridade e rastreabilidade). Antes de escrever asserts para qualquer caso marcado `Pendente`, obter decisão explícita e atualizar a fonte correspondente e este plano. Em seguida, percorrer os demais RF e os gates RNF, mantendo a rastreabilidade da RTM.