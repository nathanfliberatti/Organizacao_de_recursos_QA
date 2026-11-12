# Resultado de desempenho — rodada 03 (com volume de dados)

**Decisão: critério RNF-PERF-01 atendido neste ambiente local com 200.000 reservas já no banco**, e integridade da disputa confirmada. Vale somente para o ambiente abaixo; **não** prova desempenho em produção. Complementa a [rodada 02](resultado-02.md) (banco vazio) para atender ao aspecto "volume de dados" do RNF-06.

## Execução e rastreabilidade

| Item | Valor |
|---|---|
| Commit da execução | `2e433af8b3d9eaf82dbbcc0a03c05d86998ad7aa` (branch da entrega final; **o código de produção é o mesmo do commit `d0447b0`** — só mudam testes, documentação e o script E2E) |
| Início / fim | 2026-11-12 00:34:11Z / 00:39:17Z (300 s: 30 s de ramp-up + 270 s estáveis) |
| Plano | [`perf/reservas.jmx`](../../perf/reservas.jmx), 50 usuários, `-Jusers=50 -JrampUp=30 -Jduration=300`, JMeter 5.6.3 |
| Dados antes da carga | 200.000 reservas (180.000 ativas), 50.000 tentativas recusadas, ~78 MB — gerados por [`perf/gerar-volume.sql`](../../perf/gerar-volume.sql) |
| Aplicação / banco / máquina | Spring Boot 3.4.5, Java 21.0.12, perfis `perf,postgres`; PostgreSQL 16.15 local; VM Linux x86_64, 4 vCPU, 16 GiB; **aplicação, banco e JMeter na mesma máquina** |
| Dados brutos | `rodada-03.jtl`, 185 MB, SHA-256 `4eb55170f04cc9f99093f4b12a43df4cb989711947860f23a1fef05d4c2d6b2e`. **Não versionado** (tamanho); resumo em [`rodada-03/statistics.json`](rodada-03/statistics.json) |

## Resultados por operação

| Operação | Amostras | Média | Mediana | p90 | **p95** | p99 | Máx. | Erros inesperados | Meta p95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Consultar disponibilidade | 597.096 | 14,2 ms | 14 ms | 28 ms | **33 ms** | 44 ms | 126 ms | 0 (0,00%) | ≤ 300 ms ✔ |
| Criar reserva (sala compartilhada) | 255.896 | 22,3 ms | 21 ms | 38 ms | **44 ms** | 58 ms | 124 ms | 0 (0,00%) | ≤ 300 ms ✔ |
| Login (perfil `perf`) | 50 | 9,3 ms | 6 ms | 10,9 ms | 13 ms | 162 ms | 162 ms | 0 | — |
| **Total** | 853.042 | 16,7 ms | 16 ms | 31 ms | 36 ms | 48 ms | 162 ms | 0 (0,00%) | — |

Vazão média: 2.845 req/s (1.992 de consulta e 854 de criação). Mistura: 70,0% consulta / 30,0% criação.

## Integridade da disputa

Criações: **1 × `201`** e **255.895 × `409`**, nenhuma outra resposta. Consulta ao banco ao fim: 1 reserva ativa em `room-shared`, **255.895 tentativas recusadas** registradas para ela (igual ao número de `409`) e 200.001 reservas no total (200.000 do volume inicial + 1 aceita). O banco passou de ~78 MB para ~107 MB.

## Comparação com a rodada 02

| | Rodada 02 (banco vazio, 720 s) | Rodada 03 (200 mil reservas, 300 s) |
|---|---:|---:|
| p95 consulta de disponibilidade | 36 ms | 33 ms |
| p95 criação | 49 ms | 44 ms |
| Erros inesperados | 0 | 0 |

O volume de 200 mil reservas **não degradou** as consultas nesta máquina, o que é coerente com as consultas usarem os índices das constraints `EXCLUDE` (`gist`) por sala e por professor. Como as duas rodadas diferem em duração, ramp-up e ruído do ambiente, a pequena diferença entre elas **não** deve ser lida como melhora.

## Limitações

- Máquina única, sem isolamento entre gerador de carga, aplicação e banco.
- As consultas de disponibilidade e as criações miram **uma sala**; as 200 mil reservas do volume ficam em outras salas, então o teste mostra que o volume não atrapalha esse caminho, **não** que consultas amplas (listar muitas reservas, relatório mensal) escalem. Essas não foram medidas.
- 300 s no total (270 s estáveis), menos que os 10 minutos estáveis do RNF-PERF-01; a rodada 02 cobre os 10 minutos com banco vazio.
- Sem repetição das rodadas, não há medida de variabilidade entre execuções.
