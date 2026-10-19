# Requisitos Funcionais

## Objetivo

Definir os comportamentos obrigatórios da aplicação de organização e alocação de salas, professores e materiais, com foco em evitar conflitos de horário e permitir verificar os resultados.

Os requisitos abaixo foram derivados da solicitação do projeto. Cada requisito possui identificador estável para apoiar rastreabilidade entre implementação, testes e evidências.

## Requisitos

### RF-01 — Autenticação e autorização por perfil

O sistema deve autenticar usuários e permitir ou negar ações conforme o perfil autorizado.

**Verificação:** demonstrar acesso permitido às ações autorizadas e acesso negado às ações não autorizadas para cada perfil configurado.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-02 — Cadastro e consulta de recursos

O sistema deve permitir cadastrar e consultar salas, professores e materiais.

**Verificação:** cadastrar um recurso de cada tipo e confirmar que cada um pode ser localizado e consultado.

**Persona relacionada:** [Administrador](personas/persona-administrador.md).

### RF-03 — Pesquisa de recursos

O sistema deve permitir pesquisar recursos por tipo, capacidade, localização, competência e disponibilidade, conforme os atributos aplicáveis a cada tipo de recurso.

**Verificação:** executar pesquisas com cada critério e confirmar que os resultados correspondem aos critérios informados.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-04 — Gerenciamento de reservas

O sistema deve permitir ao solicitante criar, alterar e cancelar suas próprias reservas. As permissões administrativas devem respeitar o perfil de Administrador descrito em [persona-administrador.md](personas/persona-administrador.md); não se presume que esse perfil possa cancelar reservas de terceiros.

**Verificação:** criar uma reserva, alterar seus dados e cancelá-la; confirmar o estado resultante em cada etapa.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md).

### RF-05 — Detecção de sobreposição de horários

O sistema deve detectar sobreposição entre reservas de recursos e na agenda do professor associado à reserva.

**Verificação:** tentar registrar reservas com períodos sobrepostos para o mesmo recurso e para o mesmo professor; confirmar que cada conflito é identificado. Confirmar também que períodos sem sobreposição não são sinalizados como conflito.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-06 — Proteção contra dupla reserva simultânea

O sistema deve impedir que solicitações simultâneas confirmem reservas incompatíveis para o mesmo recurso ou professor.

**Verificação:** enviar solicitações conflitantes simultaneamente e confirmar que no máximo uma reserva incompatível é confirmada, com retorno compreensível para a solicitação não atendida.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-07 — Aprovação de recursos restritos

O sistema deve exigir aprovação de um usuário com perfil Responsável para solicitações que utilizem recursos classificados como restritos.

**Verificação:** solicitar um recurso restrito e confirmar que a reserva não é tratada como aprovada antes da decisão de um usuário autorizado; confirmar que uma decisão fica registrada.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-08 — Bloqueio de recursos em manutenção

O sistema deve permitir bloquear recursos durante períodos de manutenção e tratá-los como indisponíveis nesse intervalo.

**Verificação:** registrar um período de manutenção e confirmar que o recurso não aparece como disponível nem pode ser reservado nesse intervalo.

**Persona relacionada:** [Administrador](personas/persona-administrador.md).

### RF-09 — Retirada e devolução de materiais

O sistema deve permitir registrar e consultar a retirada e a devolução de materiais e equipamentos.

**Verificação:** registrar uma retirada e uma devolução e confirmar que o estado da movimentação pode ser consultado.

**Personas relacionadas:** [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-10 — Histórico auditável de mudanças

O sistema deve manter um histórico auditável das mudanças realizadas nos registros gerenciados pela aplicação. Toda mudança de estado de uma reserva deve gerar um registro de auditoria.

**Verificação:** realizar mudanças representativas e consultar registros que permitam identificar a ação, o registro afetado, o usuário responsável e quando a mudança ocorreu.

**Personas relacionadas:** [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-11 — Notificações

O sistema deve notificar os eventos de criação de reserva, alteração de reserva e cancelamento de reserva. A entrega pode ser simulada ou integrada a uma API externa.

**Verificação:** demonstrar o envio ou a simulação de uma notificação para cada um dos três eventos e registrar o resultado observável.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-12 — Relatórios de utilização e conflitos evitados

O sistema deve gerar relatórios mensais de utilização por recurso, carga horária alocada e conflitos evitados.

**Verificação:** gerar um relatório para um mês com dados de reservas e conflitos de teste e conferir se ele apresenta utilização por recurso, carga horária alocada e conflitos evitados.

**Personas relacionadas:** [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-13 — Validação do intervalo da reserva

O sistema deve rejeitar uma reserva cujo horário de término seja igual ou anterior ao horário de início.

**Verificação:** usar testes parametrizados para confirmar a rejeição de intervalos iguais ou invertidos e a aceitação de um intervalo válido.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Administrador](personas/persona-administrador.md).

### RF-14 — Ciclo de vida da reserva

O sistema deve suportar o fluxo principal de estados `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA` e os estados alternativos `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`.

**Verificação:** testar o fluxo principal e a representação de cada estado alternativo. As transições permitidas entre estados alternativos e os perfis autorizados a executá-las devem ser definidos antes da implementação.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Responsável](personas/persona-responsavel.md), [Administrador](personas/persona-administrador.md).

### RF-15 — Preservação de reservas iniciadas

O sistema não deve permitir apagar uma reserva depois que ela entrar no estado `EM_USO`; seu registro deve permanecer disponível para consulta e auditoria.

**Verificação:** tentar apagar uma reserva em `EM_USO` e confirmar que a operação é negada e que o registro continua consultável.

**Personas relacionadas:** [Solicitante](personas/persona-solicitante.md), [Administrador](personas/persona-administrador.md).

## Rastreabilidade

| Origem na solicitação | Requisitos |
|---|---|
| Itens 1 a 7 da lista inicial | RF-01 a RF-07 |
| Itens 1 a 5 da lista complementar | RF-08 a RF-12 |

## Decisões pendentes

- Definir a matriz detalhada de permissões, em especial quem inicia, conclui ou marca uma reserva como `NÃO_COMPARECEU`, e as transições permitidas para `REJEITADA` e `CANCELADA`.
- Definir quais atributos podem ser pesquisados para cada tipo de recurso.
- Definir quais recursos são restritos; solicitações restritas devem ser aprovadas pelo perfil Responsável.
- Escolher entrega simulada ou integração externa para notificações; os eventos já definidos são criação, alteração e cancelamento de reserva.
- Definir fórmulas de utilização, carga horária e conflitos evitados, além do fuso horário usado para fechar o mês dos relatórios.
- Definir os dados obrigatórios e o prazo de retenção do histórico auditável.
- Definir como tratar reservas existentes quando um período de manutenção for cadastrado.
- Definir a regra para reservas cujos horários apenas encostam nos limites do intervalo.