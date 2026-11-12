# DEF-012: Perfil `perf,postgres` não sobe sem credenciais por ambiente

- **Severidade / prioridade:** Média / P2 — Procedimento de carga documentado deixou de funcionar; afeta a evidência de desempenho, não o produto.
- **Situação:** Mitigado por procedimento documentado em [`docs/performance/rnf.md`](../performance/rnf.md); o comportamento do código foi mantido de propósito (ver Correção)
- **Encontrado em:** 2026-11-10, ao preparar a rodada 02 do teste de carga
- **Evidência:** com `SPRING_PROFILES_ACTIVE=perf,postgres` e sem `DB_USER`/`DB_PASSWORD`, a aplicação falha na inicialização (`Failed to execute database script`) e o JMeter registra **100% de erro** (608.117 amostras com erro em 18 s). Com as variáveis informadas, a aplicação sobe e a carga roda sem erro.
- **Causa:** após o PR #8, `application-postgres.properties` define `spring.datasource.password=${DB_PASSWORD}` **sem valor padrão** (decisão para não manter segredo no repositório). Por ser o último perfil ativo, ele sobrepõe as credenciais fixas de `application-perf.properties`.
- **Risco:** o procedimento documentado até então (`perf,postgres` sem variáveis) deixa de funcionar, e quem rodar a carga pode concluir, erradamente, que o sistema está com problema de desempenho.
- **Armadilha relacionada:** rodar só com o perfil `perf` (sem `postgres`) usa armazenamento em memória, aceita toda criação (`201` sem nenhum `409`, banco vazio) e **não valida contenção**. Uma tentativa assim foi descartada na rodada 02.
- **Correção:** o procedimento correto (`DB_USER=perf DB_PASSWORD=localperf` com `perf,postgres`) foi documentado em `rnf.md` e `resultado-02.md`. Não se voltou a embutir uma senha padrão para manter o repositório sem segredos (achado de segurança do SonarCloud no PR #8).
- **Teste de regressão:** não há teste automatizado; a verificação é a própria execução da carga, descrita em [`resultado-02.md`](../performance/resultado-02.md).
