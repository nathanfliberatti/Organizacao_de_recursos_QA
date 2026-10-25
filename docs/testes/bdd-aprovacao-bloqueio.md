# BDD — Aprovação, manutenção e estados de reserva

## Base e convenções

Cenários derivados de [Requisitos Funcionais](../requisitos-funcionais.md), da [Matriz de Rastreabilidade](../matriz-rastreabilidade.md) e da [RTM atual](../rtm.md). `docs/prd.md` não existe neste workspace; o código de API e os testes Cucumber foram adicionados após a redação inicial deste documento.

Os cenários abaixo descrevem comportamento esperado, não evidência de implementação. Onde a fonte deixa a política em aberto, a expectativa está marcada como pergunta e não deve virar um assert de aprovação até que haja decisão. Códigos HTTP específicos também não são definidos pelos requisitos.

## Recurso restrito exige aprovação de Responsável

**RF-07 — Aprovação antes de considerar aprovada**

- **Dado** que uma reserva solicitada utiliza um recurso classificado como restrito
- **E** que a decisão de um Responsável ainda não foi registrada
- **Quando** o sistema consulta o estado da reserva
- **Então** a reserva não deve ser tratada como aprovada

**RF-07 — Decisão por Responsável**

- **Dado** que existe uma solicitação pendente para um recurso restrito
- **Quando** um usuário com perfil Responsável aprova a solicitação
- **Então** a decisão deve ser registrada
- **E** a reserva deve refletir a aprovação
- **E** a mudança de estado deve gerar auditoria com ação, reserva, usuário e data/hora, conforme RF-10

## Solicitante não aprova a própria reserva

**RF-01 / RF-07 — Impedir autoaprovação**

- **Dado** que uma reserva foi criada por um Solicitante e aguarda decisão
- **Quando** a mesma identidade tenta aprovar essa reserva
- **Então** a operação deve ser negada
- **E** o estado e a decisão da reserva devem permanecer inalterados

**API — Impedir autoaprovação**

- **Dado** que a reserva restrita foi criada pela identidade autenticada
- **E** que essa identidade possui perfil Responsável
- **Quando** ela tenta aprovar a própria reserva pela API
- **Então** a resposta não deve indicar aprovação
- **E** o estado deve permanecer `SOLICITADA`
- **E** a decisão deve permanecer ausente

**RF-07 — Aprovação por outro Responsável**

- **Dado** que uma reserva aguarda aprovação e o usuário autenticado é um Responsável diferente do Solicitante
- **Quando** esse Responsável aprova a solicitação
- **Então** a decisão deve ser aceita e registrada
- **E** a reserva não deve permanecer pendente de aprovação

**RF-07 — Solicitante não pode aprovar solicitação restrita**

- **Dado** que uma reserva restrita aguarda decisão e o usuário autenticado tem perfil Solicitante
- **E** que sua identidade é diferente da identidade que criou a reserva
- **Quando** esse usuário tenta aprovar a solicitação
- **Então** a operação deve ser negada
- **E** o estado e a decisão da reserva devem permanecer inalterados

> A regra solicitada proíbe autoaprovação. Os requisitos exigem perfil Responsável para aprovar, mas não definem se uma identidade pode acumular perfis; o teste de autoaprovação deve comparar a identidade do autor da reserva com a identidade do aprovador, sem presumir a política de acumulação de perfis.

## Recurso em manutenção não pode ser reservado

**RF-08 — Bloquear reserva durante manutenção**

- **Dado** que um recurso possui período de manutenção registrado
- **E** que o período solicitado para reserva contém tempo dentro da manutenção
- **Quando** o Solicitante tenta reservar o recurso
- **Então** a reserva deve ser recusada
- **E** o recurso deve ser tratado como indisponível para esse período

### Exemplos de fronteira

Exemplo no mesmo dia: manutenção de `10:00:00` a `11:00:00`. A tabela usa precisão de segundos. Os resultados dos dois casos de contato exato permanecem em aberto, conforme RF-05/RF-08 e a decisão pendente nos requisitos funcionais.

| Caso | Período solicitado | Resultado BDD | Pergunta ou fundamento |
|---|---|---|---|
| Reserva termina exatamente no início da manutenção | `09:00:00–10:00:00` | **Em aberto** | Um intervalo de reserva que termina no instante inicial da manutenção é considerado conflito ou os períodos adjacentes são permitidos? |
| Reserva começa exatamente no fim da manutenção | `11:00:00–12:00:00` | **Em aberto** | Um intervalo de reserva que começa no instante final da manutenção é considerado conflito ou os períodos adjacentes são permitidos? |
| Reserva cruza o início da manutenção por um segundo | `09:59:59–10:00:01` | **Recusar** | Há um segundo de sobreposição dentro da manutenção (`10:00:00–10:00:01`); RF-08 determina indisponibilidade durante o período. A decisão sobre contato exato com o limite continua em aberto. |

## Ciclo de vida da reserva

**RF-14 — Fluxo principal permitido**

- **Dado** que uma reserva está no estado `SOLICITADA`
- **Quando** percorre, em ordem, as transições para `APROVADA`, `EM_USO` e `CONCLUÍDA`
- **Então** cada transição deve ser aceita
- **E** o estado final deve ser `CONCLUÍDA`
- **E** cada mudança de estado deve gerar registro de auditoria, conforme RF-10

**API — Fluxo principal e auditoria**

- **Dado** que uma reserva restrita foi solicitada e aguarda decisão
- **Quando** um Responsável distinto aprova, inicia e conclui a reserva pelas operações públicas
- **Então** os estados observados devem seguir `APROVADA`, `EM_USO` e `CONCLUÍDA`
- **E** a resposta final deve expor os três registros de transição com ator e instante

**RF-14 — Não permitir salto no fluxo principal**

- **Dado** que uma reserva está em `SOLICITADA`
- **Quando** se tenta alterá-la diretamente para `EM_USO`, sem passar por `APROVADA`
- **Então** a transição deve ser recusada
- **E** o estado deve permanecer `SOLICITADA`

**RF-14 — Reserva concluída não aceita nova aprovação**

- **Dado** que uma reserva percorreu o fluxo principal até `CONCLUÍDA` e possui decisão de aprovação registrada
- **Quando** um Responsável tenta aprová-la novamente
- **Então** a reserva deve permanecer `CONCLUÍDA`
- **E** a decisão anteriormente registrada deve permanecer inalterada

**RF-14 — Estados alternativos**

- **Dado** que uma reserva pode terminar em `REJEITADA`, `CANCELADA` ou `NÃO_COMPARECEU`
- **Quando** se tenta aplicar uma dessas alternativas
- **Então** o resultado de aceitação ainda não pode ser especificado: os estados são listados em RF-14, mas os estados de origem, os perfis autorizados e as transições correspondentes estão pendentes de definição

### Perguntas em aberto para completar a matriz de transições

1. A partir de quais estados uma reserva pode ir para `REJEITADA`, `CANCELADA` ou `NÃO_COMPARECEU`?
2. Quais perfis podem executar cada transição, inclusive iniciar, concluir, cancelar e marcar não comparecimento?
3. As alternativas são estados terminais? Há transições de saída ou reabertura permitidas?
4. Em quais estados o Solicitante pode alterar ou cancelar a própria reserva?

Até essas respostas serem incorporadas aos requisitos, não criar asserts sobre origem, ator ou reversibilidade das transições alternativas.

## Estratégia de testes

A classificação abaixo segue os comportamentos definidos e a estratégia da RTM. Testes de unidade validam regras isoladas; testes de API verificam autenticação/autorização e o comportamento observável do fluxo. A RTM prevê integração para manutenção e ciclo de vida; portanto, além dos testes unitários da regra temporal, a indisponibilidade persistida deve ser exercitada por integração. Os cenários pendentes só viram testes de aceitação após decisão.

### Testes de unidade

- **U-01:** permitir, em ordem, `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`.
- **U-02:** rejeitar salto de estado fora do fluxo principal definido, como `SOLICITADA → EM_USO`.
- **U-03:** recusar período de reserva com sobreposição positiva com manutenção, incluindo o exemplo de um segundo.
- **U-04 (após decisão):** parametrizar os casos de reserva que termina no início e começa no fim da manutenção com a política aprovada.
- **U-05 (após decisão):** validar transições de/para `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` conforme matriz aprovada.

### Testes de API

- **API-01:** reserva restrita não aparece como aprovada antes da decisão; Responsável distinto aprova e a decisão/estado ficam observáveis.
- **API-02:** Solicitante tenta aprovar a própria reserva; operação negada e estado inalterado.
- **API-03:** reserva em intervalo de manutenção é recusada e recurso permanece indisponível; cobrir a sobreposição de um segundo.
- **API-04:** executar o fluxo principal de estados pelos comandos públicos e conferir estados/auditoria resultantes.
- **API-05 (após decisão):** exercitar os dois contatos exatos com manutenção e as transições alternativas, usando atores, origens e resultados formalmente definidos.

Não exigir um código HTTP específico nos cenários até que o contrato da API o defina; verificar a decisão semântica (aceita/recusada) e a ausência de mudança indevida no estado.
