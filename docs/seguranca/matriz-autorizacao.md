# Matriz de autorização da API

## Escopo e interpretação

Esta matriz inventaria as operações declaradas nos controllers e compara a
política esperada com o código atual. Os códigos indicam sucesso esperado para
uma solicitação válida ou a resposta de autenticação/autorização: `200`, `201`,
`204`, `401` ou `403`. Erros de validação e de regra de negócio, como `400` e
`409`, não são representados nas células.

Para operações sobre uma reserva, “própria” significa que o identificador do
usuário autenticado corresponde ao solicitante/proprietário persistido na
reserva. Solicitante só pode alterar, cancelar, iniciar ou concluir a própria
reserva. A política de leitura por Solicitante não foi especificada; a matriz
espera `200` para uma consulta válida autenticada. Aprovação de reserva restrita
é exclusiva do Responsável — Administrador não substitui esse papel.

As células abaixo representam o contrato da API. A suíte
`AuthorizationMatrixIT` percorre cada célula com MockMvc, JWTs HS256 assinados
por perfil e os perfis `perf` e `postgres` ativos. No perfil `perf`, o filtro
JWT exige autenticação em todas as rotas, converte a claim assinada `roles` em
authorities Spring e deixa somente o login público. O perfil `postgres` habilita
o relatório e a persistência usados pela suíte.

## Operações

| Operação | Anônimo | Solicitante | Responsável | Administrador | Código atual / divergência |
|---|---:|---:|---:|---:|---|
| `POST /api/auth/login` *(perfil `perf`)* | 200 | 200 | 200 | 200 | Rota pública; credenciais inválidas resultam em `401`. O token de login local recebe perfil Solicitante. |
| `GET /api/reservations/restricted` | 401 | 200 | 200 | 200 | Qualquer identidade autenticada pode consultar o estado inicial demonstrativo. |
| `POST /api/reservations/restricted` | 401 | 201 | 201 | 201 | Retorna `201`; a identidade autenticada fica registrada como solicitante. |
| `GET /api/reservations/{id}` | 401 | 200 | 200 | 200 | Não há restrição de posse para leitura. Para reserva inexistente, a resposta ainda depende do tratamento de ausência no catálogo. |
| `GET /api/reservations/{id}/history` | 401 | 403 | 200 | 200 | `@PreAuthorize` permite Responsável e Administrador; JWTs assinados são convertidos em authorities. |
| `DELETE /api/reservations/{id}` *(cancelamento/exclusão lógica)* | 401 | 204 própria; 403 alheia | 204 | 204 | Solicitante é validado contra o proprietário persistido. Reservas iniciadas/concluídas produzem `409`. |
| `POST /api/reservations/{id}/approval` | 401 | 403 | 200 | 403 | `@PreAuthorize` restringe a operação ao Responsável; Administrador não substitui esse papel. |
| `POST /api/reservations` | 401 | 201 | 201 | 201 | Retorna o `reservationId` criado para permitir alteração posterior; conflito de agenda resulta em `409`. |
| `GET /api/resources/{resourceId}/availability` | 401 | 200 | 200 | 200 | Qualquer identidade autenticada pode consultar disponibilidade. |
| `POST /api/reservations/{id}/start` | 401 | 200 própria; 403 alheia | 200 | 200 | A posse é verificada antes da transição; o teste também confirma o estado `EM_USO` em transição válida. |
| `POST /api/reservations/{id}/complete` | 401 | 200 própria; 403 alheia | 200 | 200 | A posse é verificada antes da transição; o teste também confirma o estado `CONCLUIDA` em transição válida. |
| `POST /api/resources/{resourceId}/maintenance` | 401 | 403 | 403 | 201 | `@PreAuthorize` restringe ao Administrador e o controller retorna `201`. |
| `GET /relatorios/utilizacao` *(perfil `postgres`)* | 401 | 403 | 200 | 200 | Disponível no perfil `postgres`; Responsável e Administrador podem consultar. |
| `PUT /api/reservations/{id}` | 401 | 200 própria; 403 alheia | 200 | 200 | Atualiza `startsAt`/`endsAt`; o proprietário é persistido e validado para Solicitante. |
| `POST /api/resources` | 401 | 403 | 403 | 201 | Cadastro persistido permitido somente a Administrador; `id` obrigatório e `capacity` positiva. |

### Regra de posse e transições

- **Consulta:** a política de posse para leitura não foi especificada no escopo
  desta matriz. A rota não aplica restrição de posse.
- **Alteração e cancelamento:** Solicitante deve receber `200`/`204` somente
  para a própria reserva; para reserva alheia, `403`. `PUT` altera o período e
  `DELETE` realiza cancelamento/exclusão lógica.
- **Aprovação restrita:** somente o papel `RESPONSAVEL` deve efetivar a
  transição. Solicitante, Administrador e anônimo recebem `403`/`401`.
- **Início e conclusão:** são mutações da reserva e devem observar a posse do
  Solicitante. Responsável e Administrador podem operar qualquer reserva.

## Rotas sem regra específica por operação

As rotas abaixo não têm `@PreAuthorize` próprio; no perfil `perf`, são cobertas
por `anyRequest().authenticated()`. Regras de posse para início e conclusão
também são aplicadas no serviço:

- `GET /api/reservations/restricted`
- `POST /api/reservations/restricted`
- `GET /api/reservations/{id}`
- `POST /api/reservations`
- `GET /api/resources/{resourceId}/availability`
- `POST /api/reservations/{id}/start`
- `POST /api/reservations/{id}/complete`

`POST /api/auth/login` tem regra explícita `permitAll()` no perfil `perf`;
histórico, exclusão, aprovação, alteração, cadastro de recurso, manutenção e
relatório têm regras de método ou de posse explícitas, descritas na tabela.
