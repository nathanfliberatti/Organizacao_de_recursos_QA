# Requisito não funcional de desempenho (RNF-PERF-01)

**Status:** **Aprovado pelo grupo em 2026-11-12** como critério de aceite da entrega (o grupo confirmou, na conversa de fechamento da documentação, registrar os limites propostos abaixo). Valem para o **ambiente local de máquina única** descrito nos resultados; não são um compromisso para produção.

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

## Conjunto de dados e volume

O RNF-06 exige considerar o volume de dados. O plano usa dois cenários, com o mesmo [`perf/reservas.jmx`](../../perf/reservas.jmx):

| Cenário | Dados no banco antes da carga | Parâmetros | Resultado |
|---|---|---|---|
| **A — banco vazio** (rodada 02) | Nenhuma reserva; o banco começa vazio e cresce a **1** reserva | 50 usuários, ramp-up 120 s, 720 s no total | [resultado-02.md](resultado-02.md) |
| **B — volume** (rodada 03) | **200.000 reservas** (180.000 ativas e 20.000 canceladas) em 2.000 salas, 200.000 professores e 500 solicitantes, mais 50.000 tentativas recusadas; ~78 MB | 50 usuários, ramp-up 30 s, 300 s no total (`-Jusers=50 -JrampUp=30 -Jduration=300`) | [resultado-03.md](resultado-03.md) |

Dados fixos do plano: 50 usuários fictícios em [`perf/usuarios.csv`](../../perf/usuarios.csv) (credenciais válidas só no perfil `perf`), recurso disputado `room-shared` e intervalo `2026-10-27T10:00:00Z` a `11:00:00Z`. Como a tabela `reservations` não tem chave estrangeira para `resources`, o recurso disputado não precisa ser cadastrado. O script de carga do cenário B está em [`perf/gerar-volume.sql`](../../perf/gerar-volume.sql).

## Pré-condição de autenticação e execução

A aplicação fornece o endpoint de login e JWT somente no perfil local `perf`, vinculado a `127.0.0.1`; o CSV contém credenciais fictícias válidas apenas nesse perfil. Para validar contenção persistida, iniciar com os perfis `perf,postgres` **e informar as credenciais do banco por variável de ambiente** (o perfil `postgres` não tem senha padrão e, sendo o último perfil, sobrepõe as credenciais do `perf`):

```bash
DB_USER=perf DB_PASSWORD=localperf SPRING_PROFILES_ACTIVE=perf,postgres ./mvnw spring-boot:run
./jmeter -n -t perf/reservas.jmx -l rodada.jtl -e -o relatorio
```

Só o perfil `perf`, sem `postgres`, usa o armazenamento **em memória**, que aceita toda criação (`201` sem nenhum `409`) e **não serve para validar contenção** (ver [resultado-02](resultado-02.md#lições-da-execução)). A carga só deve ser direcionada a `127.0.0.1`/`localhost`, nunca a ambiente compartilhado.

## Evidência — rodada 01 (2026-10-26)

Execução CLI local com JMeter 5.6.3, PostgreSQL 16, 50 usuários, ramp-up de 120 segundos e duração total de 720 segundos (10 minutos estáveis). O servidor e o banco estavam no mesmo dev container, com o endpoint HTTP limitado ao loopback.

| Operação | Amostras | p95 observado | Erros |
|---|---:|---:|---:|
| Consulta de disponibilidade | 494.598 | 127 ms | 0 (0,00%) |
| Criação de reserva | 211.963 | 154 ms | 0 (0,00%) |

A mistura observada foi 70% consulta / 30% criação. As 211.963 criações retornaram uma resposta `201` e 211.962 respostas `409`; nenhuma resposta inesperada ou `5xx`. O PostgreSQL terminou com uma única reserva para o recurso e intervalo disputados. Os percentis e contagens são evidência desta configuração local, não uma garantia de desempenho em outro hardware ou ambiente. A proposta segue pendente de validação pelo grupo.

**Ressalva de rastreabilidade:** o resumo acima não pôde ser reprocessado nesta revisão porque `perf/resultados/rodada-01.jtl`, o relatório gerado e o banco da execução não estão disponíveis no checkout. Além disso, o JMX configura `duration=720` segundos com ramp-up de 120 segundos; isso não comprova os 10 minutos estáveis pedidos pelo RNF. Trate contagens e p95 acima como valores previamente registrados, não como métricas independentemente verificadas. A análise está em [resultado-01.md](resultado-01.md).
