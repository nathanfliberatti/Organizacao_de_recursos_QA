# Registro de defeitos

| ID | Título | Estado |
|---|---|---|
| [DEF-001](DEF-001-concorrencia-reservas.md) | API de reservas não persiste nem serializa concorrência | Resolvido (`87afce8`) |
| [DEF-002](DEF-002-deadlock-constraints-exclusao.md) | Deadlock em disputas simultâneas por sala/professor | Resolvido (`87afce8`) |
| [DEF-003](DEF-003-aprovacao-sem-autorizacao.md) | Aprovação sem autorização | Corrigido (`85a1106`) |
| [DEF-004](DEF-004-alteracao-de-reserva-ausente.md) | Alteração de reserva ausente | Corrigido (`91fb390`, `c205d77`) |
| [DEF-005](DEF-005-cancelamento-sem-regra-de-posse.md) | Cancelamento sem regra de posse | Corrigido (`7d230a4`) |
| [DEF-006](DEF-006-cadastro-de-recurso-ausente.md) | Cadastro de recurso ausente | Corrigido (`16fb9ae`) |
| [DEF-007](DEF-007-validacao-e-erros-de-reserva.md) | Validação e erros de reserva | Corrigido (`c8754c6`) |
| [DEF-008](DEF-008-manutencao-status.md) | Status HTTP do bloqueio de manutenção | Corrigido (`b587009`) |
| [DEF-009](DEF-009-transicoes-sem-regra-de-posse.md) | Transições sem regra de posse | Corrigido (`0e4d8f6`) |
| [DEF-010](DEF-010-criacao-restrita-status.md) | Código de sucesso da criação de reserva restrita (antes `DEF-001`) | Corrigido (`7d3c96b`) |
| [DEF-011](DEF-011-jwt-autoridades.md) | Autoridades dos perfis não extraídas do JWT (antes `DEF-002`) | Corrigido (`5639986`) |
| [DEF-012](DEF-012-perfil-perf-postgres-sem-credenciais.md) | Perfil `perf,postgres` não sobe sem credenciais por ambiente | Mitigado por procedimento documentado |
| [DEF-013](DEF-013-concorrencia-it-ids-colidem-com-ciclo-de-vida.md) | `ReservationConcurrencyIT` falha por colisão de ids com o ciclo de vida | Corrigido (`1793e76`) |

`DEF-010` e `DEF-011` foram renumerados porque `DEF-001` e `DEF-002` estavam duplicados (os dois primeiros defeitos de concorrência e os dois primeiros da matriz de autorização). As mensagens de commit antigas ainda citam os IDs originais.
