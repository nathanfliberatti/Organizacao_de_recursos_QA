# DEF-013: `ReservationConcurrencyIT` falha por colisão de ids com o ciclo de vida

- **Severidade / prioridade:** Média / P2 — CI vermelho por isolamento de teste, escondendo o resultado dos testes de concorrência; sem impacto em produção.
- **Situação:** Corrigido em `1793e76` (commit da branch; na `main` está dentro do squash `d0447b0`)
- **Encontrado em:** 2026-11-09, no CI do PR #8 (9 erros em `ReservationConcurrencyIT`)
- **Evidência:** `DuplicateKeyException ... reservation_lifecycle_pkey` ao inserir em `reservation_lifecycle`.
- **Causa:** a partir do PR #8, cada reserva criada pela API também cria um registro em `reservation_lifecycle` com o mesmo id. O teste reiniciava a identidade de `reservations` (`TRUNCATE ... RESTART IDENTITY`) mas mantinha `reservation_lifecycle`, então ids reaproveitados colidiam com registros antigos. Em produção os ids nunca reiniciam, então o defeito era do isolamento do teste.
- **Risco:** o CI ficava vermelho e escondia o resultado real dos testes de concorrência.
- **Correção:** o teste agora apaga `reservation_audit` e `reservation_lifecycle` antes de reiniciar os ids, nos dois pontos onde fazia isso.
- **Teste de regressão:** o próprio `ReservationConcurrencyIT` (11 execuções passam no CI desde então).
