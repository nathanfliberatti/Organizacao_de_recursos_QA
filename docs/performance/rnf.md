# Requisito não funcional de desempenho (proposta)

**Status:** Proposta para validação pelo grupo. Os limites abaixo não são ainda um compromisso aprovado.

## RNF-PERF-01 — Disponibilidade e criação de reservas

Em uma execução de carga contra a aplicação local, com 50 usuários virtuais simultâneos, ramp-up de 120 segundos e 10 minutos de carga estável, os endpoints de consulta de disponibilidade (`GET /api/resources/{resourceId}/availability`) e criação de reserva (`POST /api/reservations`) devem apresentar p95 de latência de até 300 ms por operação e taxa de erro inesperado inferior a 1% das amostras de cada operação.

Para a criação concorrente da mesma sala e do mesmo intervalo, `201 Created` e `409 Conflict` são respostas funcionais válidas. A asserção da criação deve reprovar qualquer outro status, especialmente `5xx`; `409` não deve ser contabilizado como erro de desempenho. Timeouts, falhas de transporte, respostas inesperadas e violações das asserções contam como erro. Relatar também os percentis e a taxa de erro separadamente por operação.

## Modelo de carga proposto

- **Usuários:** 50 virtuais, cada qual com identidade fictícia distinta do CSV local.
- **Ramp-up:** 120 segundos para iniciar os 50 usuários.
- **Duração:** 10 minutos em carga estável após o ramp-up.
- **Mistura de operações:** aproximadamente 70% consultas de disponibilidade e 30% tentativas de criação, distribuídas pelos usuários.
- **Contenção:** todas as criações disputam o mesmo recurso e intervalo futuro; portanto, uma aceitação (`201`) e conflitos (`409`) são resultados esperados.
- **Critério de desempenho:** p95 <= 300 ms em cada operação; erros inesperados < 1% em cada operação.

## Pré-condição de autenticação e execução

A aplicação fornece o endpoint de login e JWT somente no perfil local `perf`, vinculado a `127.0.0.1`; o CSV contém credenciais fictícias válidas apenas nesse perfil. Para validar contenção persistida, iniciar com os perfis `perf,postgres`. A carga só deve ser direcionada a `127.0.0.1`/`localhost`, nunca a ambiente compartilhado.

## Evidência — rodada 01 (2026-10-26)

Execução CLI local com JMeter 5.6.3, PostgreSQL 16, 50 usuários, ramp-up de 120 segundos e duração total de 720 segundos (10 minutos estáveis). O servidor e o banco estavam no mesmo dev container, com o endpoint HTTP limitado ao loopback.

| Operação | Amostras | p95 observado | Erros |
|---|---:|---:|---:|
| Consulta de disponibilidade | 494.598 | 127 ms | 0 (0,00%) |
| Criação de reserva | 211.963 | 154 ms | 0 (0,00%) |

A mistura observada foi 70% consulta / 30% criação. As 211.963 criações retornaram uma resposta `201` e 211.962 respostas `409`; nenhuma resposta inesperada ou `5xx`. O PostgreSQL terminou com uma única reserva para o recurso e intervalo disputados. Os percentis e contagens são evidência desta configuração local, não uma garantia de desempenho em outro hardware ou ambiente. A proposta segue pendente de validação pelo grupo.
