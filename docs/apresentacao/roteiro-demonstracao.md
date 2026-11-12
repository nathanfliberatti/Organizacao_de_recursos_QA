# Roteiro da demonstração (cerca de 12 minutos)

Demonstração ao vivo do sistema de Organização de Recursos. Os slides estão em [apresentacao.pptx](apresentacao.pptx). Cada etapa diz **o que fazer**, **o que dizer** e **qual requisito ou evidência** ela mostra.

## Preparação (antes de começar, 5 minutos)

1. Banco: `docker compose up -d db` (ou um PostgreSQL 16 local; ver o [README](../../README.md)).
2. Aplicação, com usuários de demonstração e uma senha que vocês escolhem:

   ```bash
   export DB_PASSWORD='<senha-do-banco>' APP_DEMO_DATA=true APP_DEMO_PASSWORD='<senha-demo>'
   SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
   ```

3. Abrir três janelas (ou perfis) do navegador em <http://localhost:8080>, uma por usuário: `admin` (Administrador), `responsavel` (Responsável) e `solicitante` (Solicitante), todos com a senha escolhida.
4. Deixar abertas, em outras abas: o CI da `main`, o painel do SonarCloud, o relatório JaCoCo e `docs/rtm.md`.
5. Plano B se o banco não subir: rodar sem banco (`APP_DEMO_DATA=true APP_DEMO_PASSWORD=... ./mvnw spring-boot:run`). O modo em memória também recusa conflito de horário, mas os dados somem ao parar e o relatório mensal não existe.

## Passo a passo

| # | Quem | O que fazer | O que dizer | Mostra |
|---|---|---|---|---|
| 1 | Administrador | Entrar. Em **Recursos**, cadastrar a sala `Sala Demo` com **recurso restrito** marcado. Pesquisar por localização. | "Quem administra cadastra e pesquisa recursos por tipo, capacidade, local, competência e disponibilidade. Marcar como restrito exige aprovação." | RF-01, RF-02, RF-03 |
| 2 | Administrador | Em **Manutenção**, bloquear a `Sala Demo` por um período. | "Durante a manutenção o recurso fica indisponível." | RF-08 |
| 3 | Solicitante | Em **Reservas**, tentar reservar a sala **dentro** do período de manutenção. Depois tentar com término **antes** do início. | "A interface avisa e a API recusa; a regra está no servidor, a tela só mostra." | RF-08, RF-13, RNF-04 |
| 4 | Solicitante | Reservar a sala fora da manutenção. A reserva nasce **SOLICITADA**. Tentar reservar o **mesmo horário** de novo. | "Recurso restrito não nasce aprovado. O mesmo horário é recusado, para sala e para professor. Com o PostgreSQL isso vale mesmo com solicitações simultâneas." | RF-05, RF-06, RF-07 |
| 5 | Responsável | Aprovar a reserva e abrir o **Histórico**. | "Só o Responsável aprova, e nunca a própria solicitação. Toda mudança de estado gera auditoria: quem, quando, de qual estado para qual." | RF-07, RF-10 |
| 6 | Solicitante | **Iniciar uso**. Ver que o botão **Cancelar** sumiu. | "Reserva iniciada não pode ser cancelada nem apagada." | RF-14, RF-15 |
| 7 | Solicitante e Responsável | Criar mais três solicitações. Responsável **rejeita** uma; aprova outra e registra **não compareceu**; Solicitante **cancela** a terceira. Mostrar os selos e que só sobra **Histórico**. Reservar de novo o horário liberado. | "Estados alternativos: rejeitada, não compareceu e cancelada. São finais, gravam auditoria e devolvem o horário à agenda." | RF-14 |
| 8 | Responsável | Em **Materiais**, registrar a retirada de um material e a devolução. Em **Notificações**, ver os eventos. | "Um material só pode estar retirado uma vez. As notificações são registradas e, se houver URL configurada, enviadas a uma API externa sem nunca derrubar a reserva." | RF-09, RF-11 |
| 9 | Responsável | Em **Relatório**, gerar o mês. | "Horas por recurso, carga por professor e conflitos evitados." | RF-12 |
| 10 | Qualquer | Reduzir a janela (375 px) e abrir o menu. | "A interface é responsiva." | RNF-04 |

## Fechamento: as evidências (3 minutos)

1. **CI verde** na `main`, ligado à tag `v1.0.0-rc1`.
2. **JaCoCo**: cobertura de linhas e de branches acima das metas de 80% e 70%.
3. **SonarCloud**: Quality Gate aprovado, sem bugs nem vulnerabilidades críticos.
4. **RTM** (`docs/rtm.md`): cada requisito liga risco, teste e evidência; mostrar a linha do RF-14.
5. **Defeitos**: 15 registrados, com severidade e prioridade; contar o caso do DEF-014 (dupla reserva em memória achada ao comparar com a especificação).
6. **Carga**: p95 de 36 ms e 49 ms, com 1 reserva aceita entre 534 mil tentativas na mesma sala.
7. **Decisão de qualidade**: liberar com ressalvas, não declarar pronto para produção, e por quê.

## Perguntas que costumam aparecer

- **Como vocês garantem que não há dupla reserva?** Constraints `EXCLUDE USING gist` no PostgreSQL mais *advisory locks* em ordem fixa (ADR-001). Mostrar `ReservationConcurrencyIT` e a carga.
- **E sem banco?** O modo em memória também recusa sobreposição, de forma atômica, mas só vale para uma instância (DEF-014).
- **Por que o CSRF está isento?** A API é stateless e só aceita o cabeçalho `Authorization: Bearer`; não há cookie nem sessão.
- **O que falta?** Ver os riscos aceitos em [decisao-qualidade.md](../decisao-qualidade.md): a matriz de estados não foi validada com usuários, o E2E roda fora do CI, a carga foi medida em máquina única, e não houve teste de intrusão.
