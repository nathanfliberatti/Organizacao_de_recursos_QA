# Resultado de desempenho — rodada 01

**Decisão: inconclusivo.** O arquivo bruto `perf/resultados/rodada-01.jtl`, o relatório do JMeter e o PostgreSQL da execução não estão disponíveis neste checkout. Portanto, não foi possível recalcular percentis, vazão ou erros, nem consultar a cardinalidade persistida. Os valores reproduzidos abaixo são apenas os que já constavam como resumo em [rnf.md](rnf.md); não foram validados contra a execução bruta.

## Ambiente

| Item | Identificação e limite da evidência |
|---|---|
| Máquina observada nesta análise | Dev container Linux x86_64 (kernel `6.8.0-1064-azure`), 2 vCPU e 7,8 GiB de memória. Não há evidência de que seja a máquina usada na rodada. |
| Aplicação | `0.1.0-SNAPSHOT`, conforme [pom.xml](../../pom.xml) do checkout analisado; não foi possível confirmar que este era o binário executado na rodada. |
| Commit | `20c0f7a21078321ee53bdd07f67e150060ef4b15`, HEAD deste checkout; associação à rodada não comprovada. |
| JMeter / PostgreSQL | O plano JMX declara JMeter 5.6.3; o resumo em [rnf.md](rnf.md) declara PostgreSQL 16. Nenhum dos dois foi verificado no ambiente da rodada. JMeter e cliente PostgreSQL não estão instalados neste ambiente de análise, e não há banco local acessível. |

Esses dados não permitem afirmar nada sobre produção nem caracterizar com segurança o ambiente em que a rodada foi executada.

## Carga

O [plano JMX](../../perf/reservas.jmx) configura 50 usuários virtuais, ramp-up de 120 segundos, consultas de disponibilidade e criações concorrentes no recurso `room-shared` e intervalo `2026-10-27T10:00:00Z`–`11:00:00Z`, com proporção pretendida próxima de 70%/30%. O resumo em [rnf.md](rnf.md) relata a mesma mistura e uma execução local em 2026-10-26.

**Duração a esclarecer:** o JMX define `duration=720` segundos para o grupo de threads, enquanto o RNF pede 120 segundos de ramp-up **mais** 10 minutos estáveis. Logo, a configuração não demonstra os 10 minutos estáveis requeridos; não se deve inferir vazão dividindo contagens por 720 segundos. Além disso, o intervalo fixo do JMX já era passado na data desta análise (2026-11-02), embora o plano o descreva como futuro.

## Resultados por operação

“Registrado no plano” significa transcrição do resumo já escrito em [rnf.md](rnf.md), não cálculo independente nem confirmação pelo JTL. Não foram estimados valores ausentes.

| Operação | Amostras registradas | p50 | p95 registrado | p99 | Vazão | Erros inesperados registrados | 409 esperado registrado |
|---|---:|---:|---:|---:|---:|---:|---:|
| `GET /api/resources/{resourceId}/availability` | 494.598 | Não disponível | 127 ms | Não disponível | Não calculável | 0/494.598 (0,00%) | Não aplicável |
| `POST /api/reservations` | 211.963 | Não disponível | 154 ms | Não disponível | Não calculável | 0/211.963 (0,00%) | 211.962 |

O resumo diz que houve uma resposta `201` e 211.962 respostas `409` nas criações. Portanto, os `409` são conflitos funcionais esperados e não devem ser somados aos erros de desempenho. Os totais e taxas acima são declarações do resumo existente; sem o JTL e as asserções/resultados da execução, não foi possível confirmar códigos, falhas de transporte, timeouts ou outras falhas. Também não é possível determinar p50, p99 ou vazão.

## Integridade da disputa no banco

**Não confirmada nesta análise.** Não há conexão com o banco da rodada nem ferramenta cliente PostgreSQL disponível. O esquema define uma constraint de exclusão que impede intervalos sobrepostos por sala, mas isso, isoladamente, não substitui a consulta ao estado final solicitado.

Na próxima execução, consultar o banco usado pela carga e verificar que a contagem de linhas que sobrepõem o intervalo disputado é no máximo uma, por exemplo:

```sql
SELECT count(*) AS reservas_sobrepostas
FROM reservations
WHERE room_id = 'room-shared'
  AND tstzrange(starts_at, ends_at)
      && tstzrange(
          '2026-10-27T10:00:00Z'::timestamptz,
          '2026-10-27T11:00:00Z'::timestamptz
      );
```

Essa consulta não foi executada; não se afirma aqui que o banco terminou com uma única reserva.

## Comparação com RNF-PERF-01

O RNF proposto exige p95 de até 300 ms e erro inesperado inferior a 1% **em cada operação**, além de 10 minutos estáveis após o ramp-up. Os p95 registrados no resumo (127 ms para disponibilidade e 154 ms para criação) e as taxas nele declaradas (0,00%) ficam dentro dos limites numéricos. Porém, esses números não podem ser verificados sem os artefatos brutos; p50, p99 e vazão não estão no resumo; a duração configurada não comprova os 10 minutos estáveis; e a cardinalidade no PostgreSQL não foi consultada. Assim, os dados disponíveis não sustentam uma decisão de atendimento integral.

## Hipótese de gargalo e próximo experimento

**Hipótese, não conclusão:** as criações podem ficar serializadas no lock consultivo da sala compartilhada. A sustentação é estrutural: o plano concentra todas as criações em `room-shared` e [`PostgresReservationStore.schedule`](../../src/main/java/br/com/organizacao/recursos/reservas/PostgresReservationStore.java) adquire um lock consultivo por sala. Não há medição disponível que comprove que esse lock foi o gargalo ou quantifique seu impacto.

**Próximo experimento:** repetir somente em `localhost`, com o JTL original e o relatório preservados, identificando máquina, binário/commit e versões efetivas. Configurar 120 segundos de ramp-up **mais** 600 segundos estáveis (840 segundos de duração total, se a duração do grupo inclui o ramp-up), usar um intervalo futuro, validar respostas da consulta e aceitar apenas `201`/`409` na criação. Calcular p50/p95/p99, vazão e erros inesperados separadamente por operação a partir das amostras e timestamps do JTL, excluindo `409` esperado; então executar a consulta de cardinalidade no banco da mesma execução. Até essa repetição, o resultado permanece inconclusivo e restrito à evidência local disponível.
