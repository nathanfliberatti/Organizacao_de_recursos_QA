# Avaliação Arquitetural — ATAM Ciclo 01

**Data da avaliação:** 2026-10-20
**Escopo:** arquitetura documental, requisitos, personas, código rastreado, testes e GitHub Actions disponíveis na branch `chore/maven-ci-bootstrap`.
**Natureza:** avaliação de evidências; não é aprovação da arquitetura, decisão de produto nem alteração dos requisitos.

## Resultado executivo

A arquitetura de referência descreve componentes lógicos e cenários detalhados, mas a implementação de produto ainda não foi iniciada. A busca na árvore fora do framework AIOX não encontrou arquivos Java, testes de produto, API, banco, pipeline batch ou artefatos de modelo. O README também declara a ausência de código e testes de domínio ([README.md](../../README.md#L14-L17)).

O `pom.xml` e o workflow apenas montam o lifecycle: o último `./mvnw -B verify` terminou com `BUILD SUCCESS`, mas compilou zero fontes e Surefire/Failsafe reportaram `No tests to run`. A CI existe localmente em [.github/workflows/ci.yml](../../.github/workflows/ci.yml#L1-L44), mas a consulta ao GitHub retornou zero workflows, zero runs e zero PRs; essa branch ainda não foi publicada. Portanto, o resultado verde não comprova nenhum atributo de qualidade do produto.

As ADRs `ADR-001` a `ADR-010` são explicitamente propostas, não decisões aceitas ([arquitetura, seção 16](../arquitetura.md#L775-L888)). A avaliação preserva essa distinção. Os 12 cenários ATAM existentes na arquitetura foram normalizados abaixo nas seis partes solicitadas; não foram inventadas metas para critérios que as fontes deixam pendentes.

## Direcionadores de negócio

| ID | Direcionador | Origem | Peso para o ciclo |
|---|---|---|---|
| D-01 | Alocar salas, professores e materiais, evitando intervalos inválidos e conflitos de agenda. | `RF-02` a `RF-06`, `RF-13`; driver arquitetural ([arquitetura](../arquitetura.md#L39-L53)). | Crítico para a utilidade central. |
| D-02 | Proteger recursos e ações por perfil; o Solicitante gerencia a própria reserva e o Responsável decide solicitações de recursos restritos. | `RF-01`, `RF-04`, `RF-07`; personas ([Solicitante](../personas/persona-solicitante.md#L1-L31), [Responsável](../personas/persona-responsavel.md#L1-L31), [Administrador](../personas/persona-administrador.md#L1-L31)). | Crítico para autorização e escopo. |
| D-03 | Manter estados de reserva auditáveis e preservar para consulta/auditoria a reserva que chegou a `EM_USO`. | `RF-10`, `RF-14`, `RF-15`, `RNF-03`. | Crítico para rastreabilidade operacional. |
| D-04 | Representar períodos de manutenção como indisponibilidade efetiva do recurso. | `RF-08`; responsabilidades de Administrador. | Crítico para disponibilidade correta. |
| D-05 | Permitir retirada/devolução de materiais, notificações de eventos de reserva e relatórios mensais de uso/conflitos. | `RF-09`, `RF-11`, `RF-12`. | Importante; detalhes de fluxo/cálculo ainda pendentes. |
| D-06 | Demonstrar segurança, desempenho e qualidade com evidências reproduzíveis. | `RNF-01` a `RNF-07` e RTM ([RNF](../requisitos-nao-funcionais.md#L11-L83), [RTM](../matriz-rastreabilidade.md#L1-L54)). | Gate de entrega; várias metas ainda incompletas. |
| D-07 | Executar job diário ao final do dia, usando somente dados novos. | Restrição registrada na arquitetura, atribuída a solicitação anterior e sem ID RF/RNF ([arquitetura](../arquitetura.md#L23-L27), [seção 3](../arquitetura.md#L39-L53)). | Direcionador condicional; precisa ser formalizado e ter “novo” definido. |

## Utility tree

Prioridades qualitativas: **Importância** indica impacto no direcionador; **Dificuldade** indica incerteza/dificuldade de demonstrar com as decisões atuais. `A` = alta, `M` = média, `B` = baixa. As classificações derivam da utility tree documentada ([arquitetura, seção 14](../arquitetura.md#L516-L541)); não são medidas de probabilidade nem estimativa de esforço.

| Utilidade / atributo | Cenário | Importância | Dificuldade | Direcionador |
|---|---|:---:|:---:|---|
| Integridade de agenda | ATAM-S01 — concorrência de reservas | A | A | D-01 |
| Integridade online | ATAM-S08 — persistência indisponível | A | A | D-01, D-03 |
| Correção do batch | ATAM-S02 — somente dados novos | A | A | D-07 |
| Recuperação batch | ATAM-S03 — falha parcial/retry | A | A | D-07 |
| Exclusão de execução duplicada | ATAM-S04 — jobs sobrepostos | A | A | D-07 |
| Qualidade/linhagem de dados | ATAM-S05 — lote degradado | A | A | D-07 |
| Integridade da fonte batch | ATAM-S09 — fonte indisponível | A | M | D-07 |
| Governança de modelo | ATAM-S06 — candidato inferior | A | A | D-07, condicional a uso de ML |
| Governança de modelo | ATAM-S07 — rollback | A | A | D-07, condicional a promoção/consumo |
| Segurança e privacidade | ATAM-S10 — acesso não autorizado | A | A | D-02, D-06 |
| Continuidade da notificação | ATAM-S11 — provedor indisponível | M | M | D-05 |
| Desempenho | ATAM-S12 — carga online | A | A | D-01, D-06 |

## Cenários de atributo de qualidade

Os valores `A/M` abaixo reproduzem a importância/dificuldade da análise ATAM existente. “Resposta arquitetural proposta” não significa que o comportamento esteja implementado ou seja critério formal quando a fonte identifica uma pendência.

### ATAM-S01 — Concorrência de reservas

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** dois ou mais Solicitantes autenticados; requisitos `RF-05`, `RF-06` e `RNF-02` ([arquitetura](../arquitetura.md#L544-L554)).

- **Estímulo:** comandos simultâneos pedem períodos incompatíveis para a mesma sala, material ou agenda de professor.
- **Artefato:** gestão de reservas, integridade temporal e persistência operacional, como componentes lógicos.
- **Ambiente:** operação normal; recurso inicialmente livre; disputa durante leitura/validação e confirmação.
- **Resposta:** requisito: aceitar no máximo uma solicitação incompatível e retornar rejeição compreensível às demais. A arquitetura recomenda fazer a garantia no commit/persistência; mecanismo ainda não escolhido.
- **Medida de resposta:** quantidade de reservas incompatíveis aceitas, com limite requerido de uma. Latência/capacidade são medidas de `RNF-06`, sem alvo aprovado.

### ATAM-S02 — Execução diária com somente dados novos

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** agendador/job da restrição descrita na solicitação anterior, sem RF/RNF formal ([arquitetura](../arquitetura.md#L555-L565)).

- **Estímulo:** chega a execução diária de fim do dia, com ou sem registros novos desde a última execução.
- **Artefato:** agendador, leitor incremental, cursor/checkpoint, dados de treino e manifesto, todos lógicos/propostos.
- **Ambiente:** operação normal; timezone, janela de atraso e duração não definidos.
- **Resposta:** a restrição requer treinar/retreinar apenas com dados novos. A arquitetura propõe identificar lote vazio e só avançar o cursor após tratamento recuperável; essas táticas não são ADRs aprovadas.
- **Medida de resposta:** comparar intervalo/cursor e identidade dos dados usados com o checkpoint anterior. Fonte, semântica de “novo” e prazo para concluir não têm critério aprovado.

### ATAM-S03 — Falha parcial e retry

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** runtime do job, fonte ou armazenamento; cenário proposto ([arquitetura](../arquitetura.md#L566-L576)).

- **Estímulo:** falha após leitura, gravação de lote ou treino, antes da conclusão das fases.
- **Artefato:** orquestrador, `run_id`, snapshot, manifesto, registry e checkpoint propostos.
- **Ambiente:** job em andamento; eventual retry sobre a mesma janela lógica.
- **Resposta:** proposta arquitetural: retomar/repetir sem duplicar, perder dados ou promover artefato incompleto; não declarar o cursor concluído antes de resultado durável. Política de recuperação ainda depende de decisão.
- **Medida de resposta:** diferença entre registros elegíveis e materializados, duplicatas por chave e coerência entre estado terminal e cursor; limites além da consistência não definidos.

### ATAM-S04 — Jobs diários sobrepostos

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** scheduler ou operador; cenário apoiado por ADR proposta ([arquitetura](../arquitetura.md#L577-L587)).

- **Estímulo:** uma execução continua ativa quando ocorre o próximo disparo ou retry manual.
- **Artefato:** lock/lease, fencing token, checkpoint e escritor do manifesto, todos mecanismos propostos.
- **Ambiente:** execução anterior lenta, travada ou sem confirmação de liberação.
- **Resposta:** proposta: impedir dois escritores da mesma janela e impedir que worker sem lease válida finalize. Não há política formal para ignorar, adiar ou bloquear o disparo concorrente.
- **Medida de resposta:** número de workers autorizados a confirmar a mesma janela; arquitetura propõe um escritor efetivo, não requisito formal aprovado.

### ATAM-S05 — Degradação da qualidade dos dados

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** produtor/fonte de treinamento; cenário proposto ([arquitetura](../arquitetura.md#L588-L598)).

- **Estímulo:** lote contém duplicatas, incompletude, inconsistência, atraso ou campos não elegíveis/sensíveis.
- **Artefato:** leitor incremental, validação, snapshot, manifesto e checkpoint.
- **Ambiente:** execução diária sem regras/thresholds de qualidade aprovados.
- **Resposta:** proposta arquitetural: registrar defeitos, evitar contaminação silenciosa e decidir quarentena/rejeição segundo política ainda ausente; não descartar dados silenciosamente ao avançar cursor.
- **Medida de resposta:** contagem/proporção aceitos, rejeitados e duplicados; registros rejeitados que chegaram ao snapshot. Valores e limiares pendentes.

### ATAM-S06 — Candidato pior que o baseline

**Prioridade:** importância `A`; dificuldade `A`; risco condicional, pois não há consumidor ML aprovado. **Fonte:** validador e autoridade a designar ([arquitetura](../arquitetura.md#L599-L609)).

- **Estímulo:** candidato inferior, inválido ou incomparável ao baseline.
- **Artefato:** candidato, snapshot/manifesto, avaliador e registry lógico propostos.
- **Ambiente:** treino terminou; finalidade, baseline, dataset, métrica e limiar não definidos.
- **Resposta:** proposta: rejeitar/não promover, preservar evidência e manter versão previamente aprovada se existir; não promover automaticamente sem critério aprovado.
- **Medida de resposta:** métrica do candidato versus baseline no conjunto aprovado. Nome, sentido, limite e significância são desconhecidos; não há teste de aceitação possível.

### ATAM-S07 — Rollback de versão de modelo

**Prioridade:** importância `A` se houver promoção/consumo; dificuldade `A`. **Fonte:** operador/governança não identificado ([arquitetura](../arquitetura.md#L610-L620)).

- **Estímulo:** versão promovida precisa ser revertida após problema posterior.
- **Artefato:** registry lógico, referência da versão ativa e manifesto de proveniência.
- **Ambiente:** pressupõe versões aprovada e promovida; esse uso ainda é hipótese, não requisito.
- **Resposta:** proposta: voltar à versão anterior aprovada, preservar histórico/auditoria e confirmar estado resultante. Se não houver consumidor ML, não classificar indisponibilidade de serving como risco presente.
- **Medida de resposta:** versões ativa antes/depois, sucesso e duração; gatilho e meta de recuperação não definidos.

### ATAM-S08 — Indisponibilidade da persistência online

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** Solicitante, Responsável ou Administrador; `RF-06`, `RF-10`, `RNF-02` e `RNF-04` ([arquitetura](../arquitetura.md#L621-L631)).

- **Estímulo:** repositório fica indisponível durante consulta, criação, alteração, aprovação ou auditoria.
- **Artefato:** aplicação, transação de reserva e persistência da auditoria.
- **Ambiente:** fluxo online ativo; SLO/disponibilidade não definidos.
- **Resposta:** requisito: não confirmar operação incompatível nem afirmar persistência que não ocorreu; erro compreensível e sem detalhe interno. A exigência de não perder auditoria deriva de `RF-10`; mecanismo atômico segue proposta ADR.
- **Medida de resposta:** operações confirmadas sem persistência/auditoria coerente, que devem ser zero pelo comportamento descrito. Disponibilidade e recuperação não têm alvo.

### ATAM-S09 — Fonte batch indisponível

**Prioridade:** importância `A`; dificuldade `M`. **Fonte:** fonte autorizada de treino ainda a definir ([arquitetura](../arquitetura.md#L632-L642)).

- **Estímulo:** a fonte não responde, leitura é parcial ou cursor não pode ser validado.
- **Artefato:** leitor incremental, checkpoint e orquestrador propostos.
- **Ambiente:** execução diária; retry e tolerância de atraso não definidos.
- **Resposta:** proposta: execução incompleta, cursor confirmado preservado e sem abrir janela posterior que produza lacuna; a política de recuperação permanece pendente.
- **Medida de resposta:** cursor antes/depois, janela recuperada, estado do job e atraso. Meta de recuperação pendente.

### ATAM-S10 — Acesso não autorizado a dados

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** usuário, operador, processo ou integração sem autorização; `RF-01`, `RNF-01`, `RNF-03` ([arquitetura](../arquitetura.md#L643-L653)).

- **Estímulo:** tentativa de consultar/exportar dados de treinamento, manifestos, logs, métricas ou artefatos sem permissão.
- **Artefato:** API/autorização, datasets, armazenamento, registry e logs, propostos.
- **Ambiente:** coleta, treino, armazenamento, diagnóstico ou integração externa; classificação de dados pendente.
- **Resposta:** requisitos gerais: negar operação sem perfil autorizado e não expor detalhes internos. Minimização, retenção e processo de incidente são recomendações; não afirmar anonimização, base legal ou conformidade LGPD.
- **Medida de resposta:** acessos negados/exposições por ativo e execução; metas, taxonomia e retenção precisam de aprovação.

### ATAM-S11 — Provedor de notificação indisponível

**Prioridade:** importância `M`; dificuldade `M`. **Fonte:** adaptador externo opcional; `RF-11` admite também simulação ([arquitetura](../arquitetura.md#L654-L664)).

- **Estímulo:** provedor falha ou demora ao receber criação, alteração ou cancelamento.
- **Artefato:** adaptador de notificação e estado da reserva.
- **Ambiente:** operação online; integração externa pode não existir.
- **Resposta:** a arquitetura propõe que falha externa não reverta/duplique reserva e que resultado fique observável; ordem, retry e notificação ao usuário não foram definidos.
- **Medida de resposta:** eventos gerados, enviados, confirmados, pendentes/repetidos. Não há SLO de entrega.

### ATAM-S12 — Avaliação de desempenho online

**Prioridade:** importância `A`; dificuldade `A`. **Fonte:** avaliador executando JMeter; `RNF-06` e `RNF-07` ([arquitetura](../arquitetura.md#L665-L675)).

- **Estímulo:** carga de consulta de disponibilidade e criação de reservas, incluindo contenção.
- **Artefato:** interface/API, disponibilidade, persistência e mecanismo concorrente, ainda lógicos.
- **Ambiente:** teste com ambiente, configuração e conjunto de dados informados; perfil de carga a definir.
- **Resposta:** produzir medição reproduzível e reportar erros/tempos sem violar integridade. Isso descreve a avaliação exigida, não desempenho já atingido.
- **Medida de resposta:** usuários concorrentes, duração, volume, tempos e taxa de erro conforme `RNF-06`; valores-alvo e aprovação pendentes.

## Táticas e abordagens encontradas na implementação

Para os cenários de prioridade alta, **nenhuma tática arquitetural de produto foi encontrada no código**, porque não há código Java, teste, API, persistência, batch ou ML no conjunto pesquisado. A evidência positiva disponível é apenas de build: o [POM](../../pom.xml#L17-L52) declara JUnit/Surefire/Failsafe, sem regras ou componentes; o [README](../../README.md#L14-L17) declara a ausência de código/testes. Não confundir esses mecanismos de CI com táticas implementadas de qualidade do produto.

| Cenário prioritário | Abordagem/tática encontrada no código | Evidência implementada | Abordagem somente documental |
|---|---|---|---|
| S01 — concorrência | Nenhuma encontrada. | Nenhum Java/persistência/teste concorrente; o POM apenas configura plugins ([POM](../../pom.xml#L26-L52)). | Fronteira de commit/transação, teste Testcontainers e constraint/serialização são propostas ([ADR-003](../arquitetura.md#L801-L810)); componentes lógicos em [C4](../arquitetura.md#L262-L305). |
| S02 — dados novos | Nenhuma encontrada. | Nenhum scheduler, fonte, cursor ou pipeline. | Watermark/janela estável e manifesto propostos ([ADR-005](../arquitetura.md#L823-L833)); desenho batch está no diagrama, não em código ([arquitetura](../arquitetura.md#L380-L437)). |
| S03 — retry/falha | Nenhuma encontrada. | Nenhum `run_id`, persistência de estado ou mecanismo de retry. | Idempotência/checkpoint recuperável propostos ([ADR-005/006](../arquitetura.md#L823-L844)). |
| S04 — jobs sobrepostos | Nenhuma encontrada. | Nenhum scheduler, lock ou fencing. | Lease/fencing e serialização por fonte/janela propostos ([ADR-006](../arquitetura.md#L834-L844)). |
| S05 — dados degradados | Nenhuma encontrada. | Nenhum validador, quarentena, manifesto ou fixture. | Resposta e métricas exploratórias no [ATAM-S05](../arquitetura.md#L588-L598); thresholds pendentes. |
| S06 — candidato inferior | Nenhuma encontrada. | Nenhum trainer, baseline, evaluator ou registry. | Separação candidato/versão aprovada e não promoção sem critérios em [ADR-007](../arquitetura.md#L845-L855). |
| S07 — rollback | Nenhuma encontrada. | Nenhum modelo nem versão ativa. | Rollback aparece apenas como cenário/proposta condicional ([ATAM-S07](../arquitetura.md#L610-L620), [ADR-007](../arquitetura.md#L845-L855)). |
| S08 — persistência indisponível | Nenhuma encontrada. | Nenhuma persistência operacional nem tratamento de falha. | Não confirmar operação sem integridade aparece no [ATAM-S08](../arquitetura.md#L621-L631); escolha transacional é proposta ([ADR-003](../arquitetura.md#L801-L810)). |
| S10 — segurança de dados | Nenhuma encontrada. | Nenhuma API, autenticação, autorização ou política de dados implementada. | Autorização de servidor e governança são propostas ([ADR-002](../arquitetura.md#L790-L800), [ADR-008](../arquitetura.md#L856-L866)). |
| S12 — desempenho | Nenhuma encontrada. | Nenhuma API ou script JMeter; não há resultado de carga. | Dimensões esperadas estão em [RNF-06](../requisitos-nao-funcionais.md#L51-L57); metas são pendentes. |

O workflow existente apenas executa `./mvnw -B verify` e tenta publicar relatórios ([CI](../../.github/workflows/ci.yml#L32-L44)); sem testes, relatórios e runs remotos, não demonstra as táticas acima. A busca de Actions em 2026-10-20 retornou “no workflows found” e lista vazia de runs. Isso é estado de observabilidade do repositório, não falha do software de reservas.

## Pontos de sensibilidade

Pontos de sensibilidade e seus vínculos estão documentados na [arquitetura, seção 14](../arquitetura.md#L676-L691). Nesta rodada, são riscos de decisão/implementação futura, não defeitos observados no código.

| ID | Ponto sensível | Efeito e vínculos |
|---|---|---|
| SP-01 | Fronteira de commit que protege reserva, conflito e auditoria. | Integridade/disponibilidade; S01/S08; D-01/D-03. Sem implementação, a invariante não foi demonstrada. |
| SP-02 | Semântica e avanço do cursor/checkpoint. | Correção e recuperação batch; S02/S03/S04/S09; D-07. Fonte e “novo” sem definição. |
| SP-03 | Chave e identidade/versionamento do delta. | Idempotência e reprodução; S03/S04/S05/S09; D-07. Nenhuma fonte de dados foi escolhida. |
| SP-04 | Critério de lote inválido e política de recuperação. | Qualidade e continuidade; S05; D-07. Critérios/quarentena são propostas. |
| SP-05 | Métrica, baseline, dataset e autoridade de promoção. | Governança do modelo; S06/S07; D-07 condicional. Não há requisito/consumidor aprovado. |
| SP-06 | Existência de consumidor ML/versão ativa. | Disponibilidade e rollback; S07; D-07 condicional. Sem requisito de inferência. |
| SP-07 | Categoria, acesso, retenção e destino dos dados. | Segurança/privacidade; S10; D-02/D-06. Conteúdo e finalidade não classificados. |
| SP-08 | Entrega síncrona ou durável de notificação. | Confiabilidade e acoplamento; S11; D-05. Simulador e API são alternativas permitidas. |
| SP-09 | Perfil e metas de carga. | Avaliação de performance; S12; D-06. `RNF-06` lista dimensões, não limiares. |

## Pontos de trade-off

O [quadro de trade-offs](../arquitetura.md#L692-L705) lista opções e custos potenciais, mas não há protótipos/código que as comparem. Os mais relevantes para direcionadores altos são:

- **Commit transacional forte versus operação degradada:** melhora D-01/D-03 e evita confirmação falsa; reduz disponibilidade de escrita quando o store falha. Ainda sem mecanismo escolhido ou teste (S01/S08, SP-01).
- **Retry/idempotência versus exactly-once:** replay facilita recuperação, mas exige identidade e deduplicação; exatamente-uma-vez só pode ser afirmado com prova da plataforma (S03/S04/S09, SP-02/SP-03).
- **Lock serializado versus paralelismo:** lock reduz disputa no checkpoint; pode atrasar a execução diária. Fontes/partições e política não foram aprovadas (S02–S04).
- **Snapshot imutável versus recomputação:** snapshot favorece reprodução, mas aumenta retenção/exposição/custo; não há política de retenção (S03/S05/S10, SP-07).
- **Quarentena versus bloqueio do cursor:** quarentena pode destravar lotes seguintes, mas arrisca excluir dados inválidos definitivamente; bloquear preserva revisão, mas pode atrasar batch. Regra/threshold pendentes (S02/S05/S09).
- **Outbox versus integração síncrona/simulador:** entrega durável pode desacoplar provedor, mas adiciona estado operacional; simulação satisfaz alternativa de `RF-11` sem provar entrega externa (S11, SP-08).
- **Promoção automática versus aprovação explícita:** automação é inviável sem baseline/métrica/autoridade aprovados; modelo e consumo não são requisitos atuais (S06/S07, SP-05/SP-06).

## Riscos, não riscos e temas de risco

Os riscos originais estão classificados qualitativamente na [arquitetura, seção 14](../arquitetura.md#L706-L748). O ciclo 01 não encontrou evidência para recalibrar probabilidade ou severidade; mantém-se as classificações da fonte como avaliação documental, não como incidente observado.

| ID | Risco ou não risco | Direcionador/cenário | Avaliação deste ciclo |
|---|---|---|---|
| R-01 | Dupla confirmação por validação fora do limite de persistência. | D-01; S01/S08; SP-01. | Alto documental; nenhuma proteção foi encontrada para confirmar ou mitigar. |
| R-02 | Checkpoint avançar fora de ordem ou deixar dados atrasados para trás. | D-07; S02/S03/S09; SP-02. | Alto documental; fonte/cursor inexistem no código. |
| R-03 | Retry/lock expirado permitir writers concorrentes. | D-07; S03/S04/S09; SP-02/SP-03. | Alto documental; lock/idempotência são propostas não implementadas. |
| R-04 | Dados inelegíveis contaminarem o treino sem proveniência. | D-07; S05/S06; SP-04/SP-05. | Alto documental; qualidade e thresholds não definidos nem implementados. |
| R-05 | Candidato inferior promovido ou rollback indisponível. | D-07 condicional; S06/S07; SP-05/SP-06. | Alto condicional; não há modelo, registry, consumidor nem requisito ML formal. |
| R-06 | Falha do store causar confirmação falsa ou transição sem auditoria. | D-01/D-03; S08; SP-01. | Alto documental; persistência/auditoria ausentes. |
| R-07 | Acesso indevido/exposição de dados em logs, snapshots ou provedores. | D-02/D-06; S10; SP-07. | Alto documental, exposição depende de classificação ainda ausente; sem controles implementados verificáveis. |
| R-08 | Falha de provedor atrasar/duplicar notificações. | D-05; S11; SP-08. | Médio documental; API externa é opcional e não está configurada. |
| R-09 | Resultados JMeter não permitirem decisão objetiva sem metas. | D-06; S12; SP-09. | Alto para aceite; impacto operacional não quantificado nem medido. |
| NR-01 | Aluno reservar. | D-02; RF-01/RF-04. | Não risco de requisito atual: não há persona/alocação de aluno. Não significa que autorização esteja implementada. |
| NR-02 | Indisponibilidade de serving ML. | D-07 condicional; S07. | Não risco presente documentado: não há requisito de inferência/consumidor. Reavaliar se esse escopo for aprovado. |
| NR-03 | Provedor externo interromper reservas. | D-05; S11. | Não é dependência obrigatória: `RF-11` permite simulação. Entrega externa continua risco condicional. |
| NR-04 | Microsserviços, banco ou nuvem específicos estarem ausentes. | D-01/D-07. | Não risco por si: arquitetura declara limites lógicos e tecnologias/topologia não decididas. |

Temas de risco da [arquitetura](../arquitetura.md#L722-L729): **RT-01 atomicidade/ordenação** reúne R-01/R-02/R-03/R-06; **RT-02 validade/governança do modelo** reúne R-04/R-05; **RT-03 finalidade e proteção de dados** reúne R-07; **RT-04 dependências e continuidade** reúne R-06/R-08; **RT-05 evidência e aceite** reúne R-09 e critérios de modelo não definidos. Os temas são agrupamentos analíticos, não novos requisitos.

## Promessas versus evidência de implementação

| Promessa/documento | Evidência de implementação | Divergência ou estado |
|---|---|---|
| C4 define interface, autorização, catálogo, reserva, integridade, aprovação, materiais, auditoria, notificação e relatórios ([arquitetura](../arquitetura.md#L262-L305)). | Nenhuma classe Java ou endpoint encontrado; README declara ausência de código/testes ([README](../../README.md#L14-L17)). | Componentes permanecem desenhos lógicos, não implementados. A arquitetura já avisa que são limites lógicos, portanto não é divergência de topologia. |
| Fluxo online ilustra autenticação, validação, transação, auditoria, notificação e rejeição segura ([arquitetura](../arquitetura.md#L308-L379)). | Não há controlador, serviço, persistência, auditoria nem adaptador no código rastreado. | Nenhuma resposta do fluxo foi demonstrada. Regras fechadas ainda não têm implementação; transições e reserva comum permanecem pendentes na própria fonte. |
| Fluxo batch ilustra job diário, delta, validação, manifesto, checkpoint e tratamento de candidato ([arquitetura](../arquitetura.md#L380-L437)). | Nenhum scheduler, fonte, trainer, checkpoint ou registry encontrado. | Diagrama é proposta; a arquitetura registra que a regra vem de solicitação anterior e precisa de formalização. ADR-005/006/007 não são decisões aceitas. |
| ADR-001 a ADR-010 descrevem abordagens de decomposição, segurança, persistência, outbox, watermark, lock, modelo, dados, observabilidade e custo ([arquitetura](../arquitetura.md#L775-L888)). | Nenhuma tática correspondente em código. | Propostas não executadas e não aprovadas; não podem ser apresentadas como arquitetura realizada. |
| `RNF-07` pede JUnit 5, Testcontainers, WireMock quando crítico, concorrência, JaCoCo, SonarCloud, CI, cobertura 80%/70%, TDD/BDD e zero itens críticos no fim ([RNF](../requisitos-nao-funcionais.md#L59-L83)). | POM declara JUnit/Surefire/Failsafe ([POM](../../pom.xml#L17-L52)); workflow executa verify e publica relatórios se existirem ([CI](../../.github/workflows/ci.yml#L32-L44)). Não há fonte/teste, JaCoCo, SonarCloud, Testcontainers, WireMock ou resultado de Actions. | Scaffold cobre somente comando de build. `verify` verde sem testes não satisfaz evidência comportamental, cobertura, concorrência, análise estática nem gate de entrega. |
| `RNF-06` pede JMeter e cinco dimensões, com metas a aprovar ([RNF](../requisitos-nao-funcionais.md#L51-L57)). | Nenhum endpoint, plano JMeter ou relatório; consulta GitHub sem runs. | Não avaliado; não há resultado de desempenho. |
| Critérios de reporte e auditoria dos RF/RNF. | Sem runtime e sem reports gerados; no último `verify`, Surefire/Failsafe disseram `No tests to run`. | Não há confirmação nem evidência de cobertura dos requisitos. |

**Runs:** consulta em 2026-10-20 retornou `gh workflow list` vazio e `gh run list` vazio; não há run verde ou vermelho nem artifact. A execução Maven local verde não é um GitHub Actions run.

## Ações priorizadas para o ciclo 02

1. **P0 — Validar semântica do núcleo online:** stakeholders fecharem matriz de permissões/transições de `RF-01`/`RF-14`, semântica de intervalos adjacentes, recursos restritos, campos mínimos e impacto de manutenção sobre reservas existentes. Registrar decisões nas fontes/RTM antes de converter pendências em critérios.
2. **P0 — Demonstrar S01/S08:** selecionar tecnologia de persistência, documentar decisão e construir um teste concorrente reprodutível com estado persistido e auditoria. Critério já definido é no máximo uma reserva incompatível; não inventar meta de latência.
3. **P1 — Formalizar ou retirar do escopo verificável o batch:** promover a restrição do job para RF/RNF/RTM; decidir fonte autorizada, fuso, cursor/“novo”, dados tardios, lote vazio/inválido e critérios de resultado. Só então validar S02–S05/S09 e escolher táticas.
4. **P1 — Decidir se ML é produto:** declarar finalidade/consumidor ou manter o treinamento como restrição isolada; se houver promoção, aprovar baseline, dataset, métricas, autoridade e rollback antes de S06/S07.
5. **P1 — Executar a trilha de evidências:** criar testes do produto a partir do plano TDD; adicionar Testcontainers/WireMock conforme dependências reais; completar JaCoCo/SonarCloud/JMeter e publicar runs. Registrar separadamente build verde sem teste versus qualidade comprovada.
6. **P2 — Fechar segurança, privacidade e operação:** classificar dados, definir acesso/retention/logging, confirmar requisitos de interface pública e aprovar metas JMeter, criticidade e exceções de cobertura.
7. **No próximo workshop ATAM:** revisitar utility tree com stakeholders, validar importância/dificuldade, executar protótipos para os pontos SP-01/SP-02 e reclassificar riscos somente com evidência; não tratar ADRs propostas como decisões até aprovação.

## Fontes e limites

Foram consultados requisitos, RTM, personas, arquitetura (incluindo cenários ATAM, trade-offs, riscos e ADRs), plano TDD atual, README, POM, workflow, árvore rastreada e GitHub Actions. Não foram encontrados ADRs separados nem código/testes de produto. A avaliação não estima probabilidade, desempenho ou risco quantitativo; thresholds ausentes continuam pendentes. Nenhum arquivo de produção ou requisito foi alterado nesta atividade.