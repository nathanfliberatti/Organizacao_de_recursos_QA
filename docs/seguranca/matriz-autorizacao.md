# Matriz de autorização da API

Contrato de autorização de **todas** as rotas da API: o que cada perfil recebe ao chamar cada operação. A tabela abaixo é a **fonte única**: os testes `AuthorizationMatrixDocumentTest` e `AuthorizationMatrixFullIT` **leem estas linhas** (não há cópia no código), então apagar ou alterar uma célula para fazer um teste passar não funciona.

## Como ler

- **Operação:** método HTTP e caminho. Os nomes das variáveis de caminho não importam (`{id}` e `{resourceId}` equivalem).
- **Cenário:** `padrão`, ou a **regra de posse** para operações sobre uma reserva: `reserva própria` (o Solicitante que chama é o dono) e `reserva alheia` (o dono é outro Solicitante).
- **Células:** o código HTTP esperado para uma solicitação **válida** feita por aquele perfil. `401` = sem credencial; `403` = autenticado sem permissão; `200`, `201` e `204` = sucesso. Erros de validação e de regra de negócio (`400`, `404`, `409`) não são representados.
- **Perfis:** Anônimo (sem token), Solicitante, Responsável e Administrador, todos com JWT real emitido pelo login.

## Operações

| Operação | Cenário | Anônimo | Solicitante | Responsável | Administrador | Controle no código |
|---|---|---:|---:|---:|---:|---|
| `POST /api/auth/login` | padrão | 200 | 200 | 200 | 200 | Rota pública (`permitAll`); credenciais inválidas dão `401` |
| `GET /api/auth/me` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()` |
| `GET /api/users` | padrão | 401 | 403 | 403 | 200 | `hasRole('ADMINISTRADOR')` |
| `POST /api/users` | padrão | 401 | 403 | 403 | 201 | `hasRole('ADMINISTRADOR')` |
| `GET /api/resources` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()` |
| `GET /api/resources/{id}` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()` |
| `POST /api/resources` | padrão | 401 | 403 | 403 | 201 | `hasRole('ADMINISTRADOR')` |
| `PUT /api/resources/{id}` | padrão | 401 | 403 | 403 | 200 | `hasRole('ADMINISTRADOR')` |
| `GET /api/resources/{id}/availability` | padrão | 401 | 200 | 200 | 200 | autenticação do filtro de segurança (`anyRequest().authenticated()`) |
| `GET /api/maintenance` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()` |
| `POST /api/resources/{id}/maintenance` | padrão | 401 | 403 | 403 | 201 | `hasRole('ADMINISTRADOR')` |
| `DELETE /api/resources/{id}/maintenance` | padrão | 401 | 403 | 403 | 204 | `hasRole('ADMINISTRADOR')` |
| `GET /api/reservations` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()`; o Solicitante só vê as próprias reservas |
| `POST /api/reservations` | padrão | 401 | 201 | 201 | 201 | filtro de segurança; conflito de agenda dá `409` |
| `GET /api/reservations/restricted` | padrão | 401 | 200 | 200 | 200 | filtro de segurança |
| `POST /api/reservations/restricted` | padrão | 401 | 201 | 201 | 201 | filtro de segurança; o autor fica como solicitante |
| `GET /api/reservations/{id}` | padrão | 401 | 200 | 200 | 200 | filtro de segurança; a política de posse para leitura não foi especificada |
| `GET /api/reservations/{id}/history` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `PUT /api/reservations/{id}` | reserva própria | 401 | 200 | 200 | 200 | `isAuthenticated()` + posse no serviço |
| `PUT /api/reservations/{id}` | reserva alheia | 401 | 403 | 200 | 200 | `isAuthenticated()` + posse no serviço |
| `DELETE /api/reservations/{id}` | reserva própria | 401 | 204 | 204 | 204 | `isAuthenticated()` + posse; reserva iniciada dá `409` (RF-15) |
| `DELETE /api/reservations/{id}` | reserva alheia | 401 | 403 | 204 | 204 | `isAuthenticated()` + posse; reserva iniciada dá `409` (RF-15) |
| `POST /api/reservations/{id}/approval` | padrão | 401 | 403 | 200 | 403 | `hasRole('RESPONSAVEL')`; o Administrador não substitui o Responsável |
| `POST /api/reservations/{id}/rejection` | padrão | 401 | 403 | 200 | 403 | `hasRole('RESPONSAVEL')` |
| `POST /api/reservations/{id}/no-show` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `POST /api/reservations/{id}/start` | reserva própria | 401 | 200 | 200 | 200 | filtro de segurança + posse no serviço |
| `POST /api/reservations/{id}/start` | reserva alheia | 401 | 403 | 200 | 200 | filtro de segurança + posse no serviço |
| `POST /api/reservations/{id}/complete` | reserva própria | 401 | 200 | 200 | 200 | filtro de segurança + posse no serviço |
| `POST /api/reservations/{id}/complete` | reserva alheia | 401 | 403 | 200 | 200 | filtro de segurança + posse no serviço |
| `POST /api/materials/{id}/withdrawals` | padrão | 401 | 403 | 201 | 201 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `POST /api/loans/{id}/return` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `GET /api/loans/{id}` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `GET /api/loans` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')` |
| `GET /api/notifications` | padrão | 401 | 200 | 200 | 200 | `isAuthenticated()`; só Responsável e Administrador veem as de todos (`all=true`) |
| `GET /relatorios/utilizacao` | padrão | 401 | 403 | 200 | 200 | `hasAnyRole('RESPONSAVEL','ADMINISTRADOR')`; existe só no perfil `postgres` |

## Regras de posse e de papel

- **Alterar e cancelar:** o Solicitante só tem sucesso na **própria** reserva; na alheia recebe `403`. Responsável e Administrador atuam em qualquer reserva.
- **Iniciar e concluir:** mesma regra de posse; só reserva `APROVADA` pode iniciar e só `EM_USO` pode concluir (outro estado deixa o estado inalterado).
- **Aprovar e rejeitar:** exclusivos do Responsável, e nunca o próprio solicitante da reserva. O Administrador recebe `403`.
- **Não comparecimento:** Responsável ou Administrador, e só a partir de `APROVADA`.
- **Reserva iniciada** (`EM_USO` ou `CONCLUIDA`) não pode ser cancelada nem apagada (`409`), para qualquer perfil (RF-15).
- **Leitura:** a política de posse para leitura de uma reserva específica não foi especificada; a matriz espera `200` para qualquer identidade autenticada. A listagem `GET /api/reservations` já limita o Solicitante às próprias reservas.

## Como a matriz é verificada

| Teste | O que garante | Roda em |
|---|---|---|
| `AuthorizationMatrixDocumentTest` | **Toda rota mapeada no código está nesta tabela** e toda linha da tabela corresponde a uma rota real (exceção explícita: o relatório, que só existe no perfil `postgres`); cada linha tem caso de teste e vice-versa; células só com códigos válidos | memória, sem Docker |
| `AuthorizationMatrixFullIT` | **Cada célula** (linha × perfil) contra a API real com PostgreSQL e JWT de cada perfil | Testcontainers |
| `AuthorizationMatrixIT` | Matriz do perfil de carga (`perf`) e validação de entrada com `ProblemDetail` seguro (sem stack trace, SQL nem nome de classe interna) | Testcontainers |

## Rotas sem regra de método própria

As rotas que não têm `@PreAuthorize` (`availability`, `POST /api/reservations`, `GET /api/reservations/{id}`, `GET` e `POST /api/reservations/restricted`, `start` e `complete`) dependem do filtro de segurança, que exige autenticação em todas as rotas exceto o login (`anyRequest().authenticated()`). A posse de `start` e `complete` é verificada no serviço de auditoria. Nenhuma rota fica sem proteção: o teste `AuthorizationMatrixFullIT` confirma o `401` do anônimo em todas elas.

## Divergências entre o código e a matriz

Nenhuma. Os defeitos de autorização encontrados e corrigidos ao longo do projeto (DEF-003, DEF-005, DEF-009 e DEF-011) estão em [../defeitos/](../defeitos/README.md).
