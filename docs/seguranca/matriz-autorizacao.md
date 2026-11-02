# Matriz de autorização da API

## Escopo e interpretação

Esta matriz inventaria as operações declaradas nos controllers e compara a
política esperada com o código atual. Os códigos indicam sucesso esperado para
uma solicitação válida ou a resposta de autenticação/autorização: `200`, `201`,
`204`, `401` ou `403`. Erros de validação e de regra de negócio, como `400` e
`409`, não são representados nas células.

Para operações sobre uma reserva, “própria” significa que o identificador do
usuário autenticado corresponde ao solicitante/proprietário persistido na
reserva. A regra de posse solicitada é aplicada a alterações e cancelamentos:
Solicitante pode alterá-la/cancelá-la somente se for própria. A política de
leitura de reserva por Solicitante não foi especificada; a matriz espera `200`
para a consulta autenticada, mas recomenda decidir se há restrição de
confidencialidade. Aprovação de reserva de recurso restrito é exclusiva do
Responsável — Administrador não substitui esse papel.

`Diverge` indica que o código não aplica a política esperada, aplica uma regra
mais ampla ou devolve outro código de sucesso. A aplicação não tem um
`SecurityFilterChain` geral próprio: `PerformanceAuthConfiguration` define
`anyRequest().authenticated()` somente no perfil `perf`. `@EnableMethodSecurity`
está habilitado por `ReservationSecurityConfiguration`, mas a ausência de regra
por operação continua sendo relevante. No perfil normal, a autenticação e os
códigos de respostas anônimas dependem da configuração automática do Spring
Boot; não há política HTTP de API explicitamente definida para assegurar `401`
em vez de redirecionamento.

## Operações

| Operação | Anônimo | Solicitante | Responsável | Administrador | Código atual / divergência |
|---|---:|---:|---:|---:|---|
| `POST /api/auth/login` *(perfil `perf`)* | 200 | 200 | 200 | 200 | Rota pública explicitamente permitida no perfil `perf`; credenciais inválidas resultam em `401`. |
| `GET /api/reservations/restricted` | 401 | 200 | 200 | 200 | Sem regra de método; somente o filtro genérico de autenticação do perfil `perf` a protege. |
| `POST /api/reservations/restricted` | 401 | 201 | 201 | 201 | **Diverge:** `createRestrictedReservation` não define `@PreAuthorize` e retorna `ReservationResponse`, portanto o sucesso atual é `200`, não `201`. O usuário autenticado é usado como solicitante. |
| `GET /api/reservations/{id}` | 401 | 200 | 200 | 200 | Sem regra de posse de leitura especificada; não há verificação de papel. Reserva inexistente pode causar erro não tratado, em vez de `404`. |
| `GET /api/reservations/{id}/history` | 401 | 403 | 200 | 200 | `@PreAuthorize` permite somente Responsável e Administrador. A autorização está explícita; o código de anônimo depende do filtro ativo. |
| `DELETE /api/reservations/{id}` *(cancelamento/exclusão lógica)* | 401 | 204 própria; 403 alheia | 204 | 204 | **Diverge:** `isAuthenticated()` não verifica posse nem papel; qualquer usuário autenticado pode tentar excluir/cancelar qualquer reserva. Reservas iniciadas/concluídas produzem `409` por regra de domínio. |
| `POST /api/reservations/{id}/approval` | 401 | 403 | 200 | 403 | **Diverge:** falta autorização explícita. Para não Responsável, o método retorna `200` sem mudar o estado, em vez de `403`; Administrador também não aprova, pois é convertido em `SOLICITANTE` pelo mapeamento de ator. |
| `POST /api/reservations` | 401 | 201 | 201 | 201 | Sem regra por papel/posse; exige `Authentication` no controller e depende do filtro global para acesso anônimo. Reserva recusada por conflito retorna `409`. |
| `GET /api/resources/{resourceId}/availability` | 401 | 200 | 200 | 200 | Sem regra de método; depende do filtro genérico de autenticação do perfil `perf`. |
| `POST /api/reservations/{id}/start` | 401 | 200 própria; 403 alheia | 200 | 200 | **Diverge:** não verifica posse nem papel. Para transição inválida, a API pode responder `200` sem alteração de estado. |
| `POST /api/reservations/{id}/complete` | 401 | 200 própria; 403 alheia | 200 | 200 | **Diverge:** não verifica posse nem papel. Para transição inválida, a API pode responder `200` sem alteração de estado. |
| `POST /api/resources/{resourceId}/maintenance` | 401 | 403 | 403 | 201 | **Diverge:** `@PreAuthorize("hasRole('ADMINISTRADOR')")` restringe corretamente o papel, mas o controller retorna `void`, cujo sucesso atual é `200`, não `201`. |
| `GET /relatorios/utilizacao` *(perfil `postgres`)* | 401 | 403 | 200 | 200 | `@PreAuthorize` limita corretamente a Responsável e Administrador. A rota não é registrada fora do perfil `postgres`. |
| `PUT /api/reservations/{id}` | 401 | 200 própria; 403 alheia | 200 | 200 | **Diverge:** rota de alteração ainda não existe. |
| `POST /api/resources` | 401 | 403 | 403 | 201 | **Diverge:** rota de cadastro de recurso e validação de capacidade ainda não existem; contrato de criação: `id` e `capacity`. |

### Regra de posse e transições

- **Consulta:** a política de posse para leitura não foi especificada no escopo
  desta matriz. A rota atual não aplica restrição de posse.
- **Alteração e cancelamento:** Solicitante deve receber `200`/`204` somente
  para a própria reserva; para reserva alheia, `403`. Não existe rota `PUT` ou
  `PATCH` de alteração no código atual. A rota `DELETE` é a operação de
  cancelamento/exclusão lógica e atualmente não valida posse.
- **Aprovação restrita:** somente o papel `RESPONSAVEL` deve efetivar a
  transição. Solicitante, Administrador e anônimo não devem aprovar. O código
  atual responde sucesso vazio de efeito para perfis não Responsável, em vez de
  negar a ação.
- **Início e conclusão:** são mutações da reserva e devem observar a posse do
  Solicitante. O controller atual não confere posse, papel, nem rejeita como
  HTTP `403` uma transição não aplicada.

## Rotas sem regra específica por operação

As rotas abaixo não têm `@PreAuthorize` nem matcher dedicado em configuração
HTTP. No perfil `perf`, elas são cobertas apenas pelo `anyRequest().authenticated()`;
isso não restringe perfis nem comprova posse. Nos demais perfis, a configuração
HTTP geral da API não está explicitamente definida no código:

- `GET /api/reservations/restricted`
- `POST /api/reservations/restricted`
- `GET /api/reservations/{id}`
- `POST /api/reservations`
- `GET /api/resources/{resourceId}/availability`
- `POST /api/reservations/{id}/start`
- `POST /api/reservations/{id}/complete`

`POST /api/auth/login` tem regra explícita `permitAll()` no perfil `perf`;
histórico, exclusão, manutenção e relatório têm regras de método explícitas,
descritas na tabela.
