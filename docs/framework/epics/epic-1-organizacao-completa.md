# Epic 1: Produto completo de organização de recursos

**Status:** Em execução — Story 1.1 `Done` com gate `CONCERNS`; Story 1.2 `Ready` e liberada para iniciar
**Escopo:** Produto funcional completo, CLI primeiro e frontend integrado ao Spring Boot depois
**Identificadores:** Epic 1 mantém compatibilidade com as stories 1.1 e 1.2 existentes

## Objetivo

Entregar uma aplicação completa para organizar recursos acadêmicos, permitindo que Solicitantes, Responsáveis e Administradores realizem seus fluxos autorizados por uma CLI funcional antes da disponibilização dos mesmos fluxos em um frontend responsivo integrado ao monólito Spring Boot. A API será a fonte única das regras de negócio, autorização e dados; os canais não poderão contorná-la.

O resultado deve atender RF-01 a RF-15 e RNF-01 a RNF-07 com evidências verificáveis, sem considerar um slice inicial ou a existência de uma rota como conclusão de um requisito mais amplo.

## Contexto e decisões vigentes

- [ADR-001](../../adr/ADR-001-integridade-concorrente-postgresql.md) define PostgreSQL como autoridade de integridade contra reservas incompatíveis, com locks transacionais ordenados e constraints de exclusão; a política de contato exato entre intervalos continua pendente.
- [ADR-002](../../adr/ADR-002-identidade-e-canais-produto.md) define o monólito Spring Boot, a API como fonte de verdade, CLI Java 21 via JDK `HttpClient` antes da UI e frontend estático same-origin em `src/main/resources/static/` depois. O framework JavaScript não foi escolhido.
- ADR-002 também define usuários persistidos em PostgreSQL, senhas BCrypt, JWT de até 15 minutos, bootstrap explícito de Administrador sem defaults, criação administrativa de usuários e verificação de usuário ativo/papéis atuais em cada requisição. O segredo `RECURSOS_JWT_SECRET` é obrigatório, sem fallback, e o login do Solicitante corresponde provisoriamente ao `professorId` canônico; não há reserva em nome de terceiro.
- As personas são [Solicitante](../../personas/persona-solicitante.md), [Responsável](../../personas/persona-responsavel.md) e [Administrador](../../personas/persona-administrador.md). A [matriz de autorização](../../seguranca/matriz-autorizacao.md) e os requisitos delimitam suas permissões; uma interface não substitui autorização server-side.
- A story [1.1 — Histórico auditável](../../stories/1.1-historico-auditavel-reservas.story.md) está `Done`, com gate `CONCERNS` porque a CLI do CodeRabbit estava indisponível; os critérios foram revalidados e `./mvnw -B verify` passou. A story [1.2 — CLI de autenticação, catálogo e reservas próprias](../../stories/1.2-cli-autenticacao-catalogo-reservas.story.md) foi validada GO 10/10 e está `Ready`; a precondição de QA foi resolvida e seu desenvolvimento pode iniciar.

## Outcomes verificáveis

1. Cada requisito funcional RF-01 a RF-15 tem comportamento implementado, testes/evidências apropriados e rastreabilidade na RTM; um primeiro fluxo parcial não fecha um requisito abrangente.
2. Os três perfis conseguem executar somente as ações autorizadas. Autorização e propriedade são verificadas pela API, inclusive quando a chamada não vem dos clientes oficiais.
3. Reservas válidas são gerenciadas ao longo dos estados permitidos, sem confirmação incompatível sob concorrência, com recursos indisponíveis respeitados e histórico de mudanças consultável conforme autorização.
4. Os fluxos públicos do produto podem ser operados pela CLI antes de sua disponibilidade na UI. A CLI usa HTTP contra a API e não acessa banco ou stores diretamente.
5. Após a cobertura equivalente pela CLI, o frontend same-origin servido pelo Spring permite executar os fluxos correspondentes pelas personas, com estados de carregamento/erro utilizáveis e comportamento responsivo demonstrado.
6. Os requisitos não funcionais têm evidência conforme seus critérios; valores-alvo e decisões ainda pendentes permanecem explicitamente em aberto até aprovação.

## Escopo

### IN

- Autenticação, gestão administrativa de usuários e autorização por perfil segundo RF-01, personas, matriz de autorização e ADR-002.
- Cadastro, consulta e pesquisa de salas, professores e materiais, com atributos e filtros aplicáveis a cada tipo definidos antes de serem tratados como completos (RF-02, RF-03).
- Criação, consulta, alteração e cancelamento de reservas próprias; validação temporal, detecção de conflitos e proteção contra dupla reserva simultânea (RF-04 a RF-06, RF-13).
- Aprovação de recursos restritos por Responsável, manutenção/bloqueio de recursos, movimentação de materiais, auditoria, notificações, relatórios mensais e ciclo de vida completo de reservas (RF-07 a RF-12, RF-14, RF-15).
- API Spring como contrato e autoridade de comportamento, consumida primeiro pela CLI e depois pelo frontend estático same-origin.
- Documentação atualizada dos fluxos públicos (RNF-05); testes unitários, integração, API caixa-preta e E2E; Testcontainers para persistência, WireMock para integração externa crítica quando aplicável, concorrência automatizada, evidência TDD/BDD, CI, JaCoCo e SonarCloud (RNF-07).
- Avaliação JMeter reproduzível de consulta de disponibilidade e criação de reservas nas dimensões especificadas em RNF-06.
- UI responsiva, mensagens compreensíveis e fluxos E2E representativos (RNF-04).

### OUT

- Qualquer cron job, treinamento ou retreinamento de modelo: não é exigido por RF-01 a RF-15 ou RNF-01 a RNF-07 e está fora do produto autorizado neste épico.
- Cadastro público, reservas por aluno ou criação de reserva em nome de outro professor; permissões administrativas sobre reservas de terceiros que não estejam definidas pelos requisitos.
- Acesso direto da CLI ao banco/stores, ou regras de negócio/autorização implementadas exclusivamente em um cliente.
- Escolher agora um framework JavaScript, provedor externo obrigatório de notificações, metas de performance ou políticas ainda pendentes nos documentos de origem.
- Expandir requisitos, personas, arquitetura ou decisões aprovadas; questões abertas devem ser resolvidas pelos responsáveis apropriados e registradas nos artefatos fonte antes de serem assumidas por stories.

## Rastreabilidade de requisitos

| Requisito | Cobertura pretendida no épico | Evidência principal / ressalva |
|---|---|---|
| RF-01 | Login e autorização por perfil em cada operação protegida | Testes de acesso permitido/negado na API para perfis e operações; detalhes de autorização pendentes devem ser definidos antes do fluxo afetado. |
| RF-02 | Cadastro e consulta de salas, professores e materiais | Testes de API/integração para cada tipo de recurso. |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade, conforme aplicabilidade | Casos parametrizados; atributos/filtros por tipo ainda precisam ser definidos. O catálogo limitado da story 1.2 não conclui RF-03. |
| RF-04 | Criar, alterar e cancelar reservas próprias | Testes de API e E2E para os três comandos e estado final; não presumir cancelamento de terceiros. |
| RF-05 | Detectar sobreposição de recurso e professor | Testes positivos e negativos para as combinações exigidas; regra de contato exato entre períodos permanece pendente. |
| RF-06 | Impedir confirmação concorrente incompatível | Teste concorrente repetível com persistência PostgreSQL via Testcontainers; no máximo uma solicitação incompatível confirmada. |
| RF-07 | Aprovação de recurso restrito por Responsável | Testar estado não aprovado antes da decisão, decisão autorizada e registro; catálogo de recursos restritos ainda precisa ser definido. |
| RF-08 | Bloqueio de recursos durante manutenção | Testar cadastro/autorização, indisponibilidade e rejeição de reserva; tratamento de reservas existentes ao cadastrar manutenção continua pendente. |
| RF-09 | Retirada, devolução e consulta da movimentação de materiais | Testes de integração/API; campos e executor exato devem seguir os requisitos e personas. |
| RF-10 | Histórico auditável de mudanças gerenciadas e de toda transição de reserva | Testar ação, registro afetado, usuário e data/hora; story 1.1 cobre mudanças de estado de reserva, sem implicar conclusão do requisito amplo. |
| RF-11 | Notificar criação, alteração e cancelamento | Demonstrar simulação ou integração externa aprovada para cada evento; modo de entrega permanece pendente. |
| RF-12 | Relatórios mensais de utilização, carga horária e conflitos evitados | Conferir relatório contra dados de teste; fórmulas e fuso para fechamento do mês permanecem pendentes. |
| RF-13 | Rejeitar término igual ou anterior ao início | Testes JUnit 5 parametrizados para intervalos inválidos e válido, além da validação observável na API. |
| RF-14 | Fluxo principal e estados alternativos do ciclo de vida | Testes de transição e autorização; matriz de transições/perfis ainda deve ser definida. |
| RF-15 | Preservar reserva após entrar em `EM_USO` | Tentativa de exclusão negada, com reserva e histórico consultáveis. |

| Requisito não funcional | Cobertura pretendida no épico | Evidência principal / ressalva |
|---|---|---|
| RNF-01 | Segurança e controle de acesso | Testes documentados por perfil e tentativas não autorizadas; mecanismo de identidade conforme ADR-002. |
| RNF-02 | Integridade sob concorrência | Execuções repetíveis com PostgreSQL/Testcontainers sem dupla confirmação incompatível; volume-alvo continua pendente. |
| RNF-03 | Rastreabilidade e auditabilidade | Registros consultáveis com ação, objeto, autor e data/hora e testes de criação; retenção/proteção/acesso continuam pendentes. |
| RNF-04 | Responsividade e clareza de mensagens | Fluxos principais e erros verificados em telas de diferentes dimensões; navegadores, acessibilidade e dimensões alvo não definidos. |
| RNF-05 | Documentação dos fluxos públicos | Documentação versionada conferida com API/fluxos realmente entregues; formato e publicação permanecem pendentes. |
| RNF-06 | Desempenho com JMeter | Plano/relatório reproduzível para disponibilidade e criação, incluindo concorrência, duração, volume, tempo de resposta e taxa de erro. Os valores-alvo devem ser acordados antes do aceite de desempenho. |
| RNF-07 | Estratégia automatizada de qualidade e gates | JUnit 5, integração/API/E2E, Testcontainers, WireMock quando aplicável, teste concorrente, evidência TDD/BDD, GitHub Actions em PR, JaCoCo mínimo de 80% de linhas e 70% de branches, SonarCloud e zero bugs/vulnerabilidades críticos na entrega final. Exclusões e fonte de criticidade ainda pendentes. |

## Critérios de sucesso do épico

- RF-01 a RF-15 permanecem rastreados a stories, testes e evidências na RTM; critérios não definidos nos requisitos não são considerados resolvidos por inferência.
- Cada fluxo público que será oferecido pela UI tem primeiro um fluxo equivalente operável pela CLI sobre a mesma API. O frontend não inicia antes da cobertura CLI/API correspondente.
- A autorização de servidor é demonstrada para perfis e operações, e a criação de reserva do Solicitante não aceita professor alternativo ao subject canônico definido provisoriamente no ADR-002.
- Testes de integração com PostgreSQL demonstram a integridade sob concorrência; as validações críticas de ciclo de vida, auditoria e preservação de reserva têm evidência comportamental.
- As interfaces de CLI e web apresentam resultados e erros compreensíveis sem expor segredos ou detalhes internos; a web é responsiva conforme evidência do RNF-04.
- RNF-05 e RNF-07 têm evidências verificáveis. A cobertura atinge os mínimos declarados, sem exclusões não documentadas; a entrega final não tem bugs críticos nem vulnerabilidades críticas abertas.
- O resultado JMeter é reproduzível e cobre todas as dimensões do RNF-06. O gate de desempenho permanece inconclusivo até que os valores-alvo sejam aprovados.
- Nenhuma story é marcada como concluída apenas por código ou cobertura agregada: seus critérios de aceite e gates próprios precisam passar.

## Sequência proposta de stories

Os itens futuros são slices conceituais para o @sm detalhar e para o @po validar. Os números a partir de 1.3 são sugestões de compatibilidade, não stories criadas ou aprovadas. Cada slice de domínio deve incluir contrato/API, comportamento CLI e testes correspondentes antes de sua experiência web.

| Ordem / ID sugerido | Slice de alto nível | Requisitos relacionados | Dependências e observações |
|---|---|---|---|
| Existente 1.1 | Histórico auditável de reservas | RF-10, RF-15; RNF-03 | `Done`, gate `CONCERNS` pela indisponibilidade do CodeRabbit; critérios revalidados e `./mvnw -B verify` passou. |
| Validada 1.2 | Primeiro slice CLI: autenticação, catálogo limitado, disponibilidade e reservas próprias | RF-01, RF-02 parcial, RF-03 parcial, RF-04 parcial, RF-05/06/13; RNF-01/02/04/05 | `Ready`, GO 10/10. A precondição de QA foi resolvida; desenvolvimento liberado para iniciar. Restrições de catálogo da story não concluem RF-03. |
| Sugerida 1.3 | Catálogo completo de recursos e pesquisa; operações de administração disponíveis pela API e CLI | RF-02, RF-03; RNF-01/05 | Depende de decisões sobre tipos, atributos e critérios de busca por recurso; pode reutilizar autenticação/API do slice 1.2 após validação. |
| Sugerida 1.4 | Fluxos de reserva próprios completos, ciclo de criação/alteração/cancelamento, validação e conflito | RF-04, RF-05, RF-06, RF-13; RNF-02/04 | Depende da identidade do solicitante e contratos API/CLI; fechar regra de limites de intervalo antes de criar expectativas de fronteira. |
| Sugerida 1.5 | Estados alternativos, autorização das transições, aprovação de restritos e preservação de reservas iniciadas | RF-01, RF-07, RF-14, RF-15; RNF-01/03 | Depende da matriz aprovada de transições/perfis e da definição de recursos restritos; integrar a cobertura existente de auditoria da 1.1. |
| Sugerida 1.6 | Cadastro e operação de manutenção/bloqueios com disponibilidade coerente | RF-08; RNF-01/02/04 | Depende de definir o tratamento de reservas existentes impactadas por manutenção; operações expostas pela API e CLI. |
| Sugerida 1.7 | Retirada, devolução e consulta de materiais | RF-09, RF-10; RNF-01/03 | Depende dos atributos/catálogo de materiais e de confirmar as permissões aplicáveis conforme personas. |
| Sugerida 1.8 | Relatórios mensais e notificações dos eventos de reserva | RF-11, RF-12; RNF-03/05 | Depende de fórmulas/fuso do relatório e decisão entre notificação simulada ou API externa; WireMock quando a integração externa for escolhida. |
| Sugerida 1.9 | Completar e verificar auditoria dos registros gerenciados e acesso aos históricos por perfil | RF-10; RNF-03 | Aproveita a story 1.1 e deve cobrir mudanças de recursos, usuários, manutenção e materiais conforme o escopo definido; retenção/proteção/acesso seguem pendentes. |
| Sugerida 1.10 | Fechamento de cobertura operacional CLI e documentação pública dos fluxos entregues | RF-01 a RF-15 conforme operações aplicáveis; RNF-05 | Depende dos slices de domínio anteriores; verificar paridade CLI/API sem adicionar comandos ou permissões fora dos requisitos. Pré-requisito para iniciar os fluxos correspondentes na UI. |
| Sugerida 1.11 | Frontend responsivo estático same-origin para as personas, integrado à mesma API Spring | RF-01 a RF-15 conforme fluxos públicos; RNF-01/04/05 | Só após a API e os fluxos CLI equivalentes estarem disponíveis; framework JS continua decisão aberta até aprovação própria. Incluir E2E dos fluxos web. |
| Sugerida 1.12 | Consolidação de evidências e gates de qualidade/desempenho do produto completo | RNF-01 a RNF-07 e RFs críticos | Integra evidências produzidas durante as stories; RNF-06 depende de alvos acordados. Não posterga testes, segurança ou quality gates para o final. |

## Dependências e decisões pendentes

- A precondição de QA da story 1.2 foi resolvida com a conclusão da 1.1; a 1.2 pode iniciar. O `CONCERNS` da 1.1 registra apenas que a revisão CodeRabbit não foi executada por indisponibilidade da CLI.
- Definir atributos/filtros de pesquisa por tipo de recurso, quais recursos são restritos, transições e perfis autorizados em todos os estados, e impactos da manutenção sobre reservas existentes.
- Definir a semântica de períodos que se tocam; não presumir que o padrão técnico `[)` do PostgreSQL encerra a decisão de produto.
- Definir fórmulas de utilização/carga/conflitos evitados, fuso mensal, entrega simulada ou externa de notificação, dados e retenção da auditoria, formato/publicação da documentação e alvos JMeter.
- Definir e justificar exclusões JaCoCo e a fonte de classificação de bugs/vulnerabilidades críticos para o gate final.
- Revalidar o vínculo provisório entre usuário Solicitante e `professorId` quando o catálogo de professores for definido; definir gestão operacional/rotação do segredo JWT sem adicionar defaults.
- A escolha do framework JavaScript e dos detalhes de UI permanece aberta; não é bloqueio para desenvolver a API/CLI, mas deve preceder sua story de frontend.
- Há divergência documental: [arquitetura](../../arquitetura.md) menciona treinamento diário, mas não há requisito correspondente nos RF/RNF; este épico o exclui conforme o escopo autorizado. Não altera arquitetura ou requisitos e não considera a divergência resolvida.

## Riscos

| Risco | Impacto | Mitigação/condição de avanço |
|---|---|---|
| CodeRabbit não esteve disponível durante o gate da 1.1 | A revisão automatizada ainda pode apontar questões adicionais antes do merge. | O gate `CONCERNS` é não bloqueante para a 1.2; iniciar seu desenvolvimento e executar a revisão automatizada antes do merge quando a CLI estiver disponível. |
| Atributos, permissões e transições ainda incompletos | Pesquisa, reserva, aprovação e UI podem codificar regras contraditórias. | Resolver as pendências nos artefatos de origem antes de stories que dependam delas. |
| Concorrência ou configuração de segurança inconsistentes entre execução e testes | Dupla reserva ou autorização indevida apesar de testes isolados passarem. | Validar API e PostgreSQL reais, perfis, bootstrap e segredo de produto; cliente não é fronteira de segurança. |
| Frontend iniciado antes de API e CLI equivalentes | Duplicação de regras e gaps entre canais, contrariando ADR-002 e CLI-first. | Bloquear o início da UI para cada fluxo até que seu contrato/API e uso CLI estejam demonstrados. |
| Metas de performance e detalhes de acessibilidade ainda não definidos | Não é possível emitir veredito quantitativo completo sobre RNF-06 nem critérios específicos além da responsividade de RNF-04. | Registrar os alvos antes da aprovação final; não inventar números durante implementação. |
| Divergência sobre treinamento na arquitetura | Planejamento pode reintroduzir trabalho que não pertence aos RF/RNF deste produto. | Manter treinamento/cron fora deste épico e encaminhar a divergência aos responsáveis por arquitetura/requisitos, sem alterar esses documentos nesta entrega. |

## Definition of Done do épico

- Todos os RFs e RNFs têm stories aceitas, evidências e estado atualizado na RTM; decisões pendentes foram resolvidas nos documentos apropriados ou permanecem explicitamente sem veredito quando bloqueiam um critério.
- Todos os fluxos públicos cobertos pela UI foram antes operáveis pela CLI e usam a mesma API/autorização Spring.
- Os fluxos web das três personas aplicáveis passam testes E2E e evidência responsiva; o canal não introduz regras próprias.
- Os gates do RNF-07 passam, inclusive cobertura mínima, concorrência automatizada, CI/SonarCloud e zero itens críticos na entrega final.
- O relatório JMeter contém as dimensões exigidas e só recebe aprovação quando comparado a alvos formalmente acordados.
- Não há treinamento/cron job de treinamento incluído como entrega deste épico.