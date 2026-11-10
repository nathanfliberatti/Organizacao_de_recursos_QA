# Resultado de desempenho — rodada 02

**Decisão: critério RNF-PERF-01 atendido neste ambiente local** (p95 ≤ 300 ms e erros inesperados < 1% em cada operação) **e integridade da disputa confirmada no banco**. O resultado vale somente para o ambiente descrito abaixo; **não** prova desempenho em produção, em outra máquina nem com a aplicação separada do banco e do gerador de carga.

## Execução e rastreabilidade

| Item | Valor |
|---|---|
| Commit da aplicação | `d0447b04893a189c71fb26376a6dfc340b3ab2dc` (`main`, merge do PR #8) |
| Início / fim | 2026-11-10 00:10:24Z / 00:22:25Z (720 s: 120 s de ramp-up + 10 min estáveis) |
| Plano | [`perf/reservas.jmx`](../../perf/reservas.jmx), 50 usuários virtuais, JMeter 5.6.3 (checksum SHA-512 do `.tgz` conferido) |
| Aplicação | Spring Boot 3.4.5, Java 21.0.12, perfis `perf,postgres` (credenciais por `DB_USER`/`DB_PASSWORD`) |
| Banco | PostgreSQL 16.15 (instalação local, mesma máquina) |
| Máquina | VM Linux x86_64 (kernel 6.18), 4 vCPU, 16 GiB; **aplicação, banco e JMeter na mesma máquina** |
| Dados brutos | `rodada-02.jtl` — 386 MB, SHA-256 `f5c017f9663c9f77c50fd61b2809bb1231246d42219100642655c84c8d922b74`. **Não versionado** (tamanho); o resumo por operação do JMeter está em [`rodada-02/statistics.json`](rodada-02/statistics.json). |

Os valores abaixo vêm de `statistics.json` (gerado pelo JMeter a partir do `.jtl`) e de consultas ao banco ao fim da execução.

## Resultados por operação

| Operação | Amostras | Média | Mediana | p90 | **p95** | p99 | Máx. | Erros inesperados | Meta p95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Consultar disponibilidade | 1.246.041 | 15,9 ms | 15 ms | 30 ms | **36 ms** | 48 ms | 128 ms | 0 (0,00%) | ≤ 300 ms ✔ |
| Criar reserva (sala compartilhada) | 534.014 | 24,7 ms | 24 ms | 42 ms | **49 ms** | 63 ms | 134 ms | 0 (0,00%) | ≤ 300 ms ✔ |
| Login (perfil `perf`) | 50 | 5,4 ms | 4 ms | 9 ms | 10,4 ms | 30 ms | 30 ms | 0 | — |
| **Total** | 1.780.105 | 18,5 ms | 18 ms | 34 ms | 40 ms | 55 ms | 134 ms | 0 (0,00%) | — |

Vazão média: 2.472 req/s no total (1.731 req/s de consulta e 742 req/s de criação). Mistura observada: 70,0% consulta / 30,0% criação.

## Integridade da disputa

Todas as criações disputaram a mesma sala (`room-shared`) e o mesmo intervalo. Respostas de criação: **1 × `201`** e **534.013 × `409`**; nenhuma outra (sem `5xx`). Consulta ao banco ao fim:

| Verificação | Resultado |
|---|---|
| Reservas ativas na sala compartilhada | **1** |
| Reservas totais | 1 |
| Tentativas recusadas registradas (`reservation_conflict_attempts`) | 534.013 (igual ao número de `409`) |

Ou seja, a aceitação foi única e cada recusa foi registrada, sob 50 usuários concorrentes (RF-05, RF-06, ADR-001).

## Comparação com a rodada 01

A [rodada 01](resultado-01.md) ficou **inconclusiva** por falta do `.jtl`; seu resumo registrava p95 de 127 ms (consulta) e 154 ms (criação) em outra versão do código. Esta rodada, com dados reprocessáveis pelo `statistics.json` e checksum do `.jtl`, mostra p95 de 36 ms e 49 ms. A diferença **não** deve ser lida como melhora do sistema: ambiente, máquina e versão do código diferem entre as rodadas.

## Lições da execução

1. **Só `perf` usa memória e não valida contenção.** Uma primeira tentativa foi feita só com o perfil `perf`: 527.518 criações devolveram `201` e nenhum `409`, e o banco ficou vazio, porque o armazenamento em memória aceita toda reserva. Esse resultado foi **descartado**. A rodada válida usa `perf,postgres`.
2. **`perf,postgres` exige credenciais por ambiente.** Após o PR #8 o perfil `postgres` não tem senha padrão e sobrepõe as credenciais do `perf`; sem `DB_USER`/`DB_PASSWORD` a aplicação não sobe e o JMeter registra 100% de erro. O procedimento correto está em [rnf.md](rnf.md).

## Limitações

- Máquina única, sem isolamento entre gerador de carga, aplicação e banco; resultados absolutos dependem desse ambiente.
- Uma única sala disputada; a maioria das criações termina em `409` (caminho de conflito), o que **não** representa a mistura real de uso (muitas reservas livres).
- Não mede o frontend, o login real (BCrypt) nem as telas; o login medido é o do perfil `perf`.
- Sem repetição da rodada: não há medida de variabilidade entre execuções.
