# Registro de defeitos

Cada defeito foi **encontrado, priorizado e corrigido** (ou mitigado, no caso de DEF-012). `DEF-014` e `DEF-015` foram achados pela comparação com a especificação oficial, já na etapa final. A severidade e a prioridade abaixo foram atribuídas na entrega final, a partir do impacto descrito em cada arquivo; a ordem cronológica de correção **não** é a prioridade.

| Escala | Significado |
|---|---|
| **Severidade** Crítica | Perda de integridade ou falha de segurança explorável em produção |
| **Severidade** Alta | Viola uma regra crítica ou um requisito obrigatório da especificação |
| **Severidade** Média | Funcionalidade ou robustez comprometida, sem violar regra crítica |
| **Severidade** Baixa | Contrato ou aparência; comportamento essencial correto |
| **Prioridade** P1 | Corrigir antes de qualquer outra entrega (bloqueia a liberação) |
| **Prioridade** P2 | Corrigir antes da entrega final |
| **Prioridade** P3 | Corrigir quando houver folga |

| ID | Título | Severidade | Prioridade | Estado |
|---|---|---|---|---|
| [DEF-001](DEF-001-concorrencia-reservas.md) | API de reservas não persiste nem serializa concorrência | Alta | P1 | Resolvido (`87afce8`) |
| [DEF-002](DEF-002-deadlock-constraints-exclusao.md) | Deadlock em disputas simultâneas por sala/professor | Alta | P1 | Resolvido (`87afce8`) |
| [DEF-003](DEF-003-aprovacao-sem-autorizacao.md) | Aprovação sem autorização | Alta | P1 | Corrigido (`85a1106`) |
| [DEF-004](DEF-004-alteracao-de-reserva-ausente.md) | Alteração de reserva ausente | Média | P2 | Corrigido (`91fb390`, `c205d77`) |
| [DEF-005](DEF-005-cancelamento-sem-regra-de-posse.md) | Cancelamento sem regra de posse | Alta | P1 | Corrigido (`7d230a4`) |
| [DEF-006](DEF-006-cadastro-de-recurso-ausente.md) | Cadastro de recurso ausente | Média | P2 | Corrigido (`16fb9ae`) |
| [DEF-007](DEF-007-validacao-e-erros-de-reserva.md) | Validação e erros de reserva | Média | P2 | Corrigido (`c8754c6`) |
| [DEF-008](DEF-008-manutencao-status.md) | Status HTTP do bloqueio de manutenção | Baixa | P3 | Corrigido (`b587009`) |
| [DEF-009](DEF-009-transicoes-sem-regra-de-posse.md) | Transições sem regra de posse | Alta | P1 | Corrigido (`0e4d8f6`) |
| [DEF-010](DEF-010-criacao-restrita-status.md) | Código de sucesso da criação de reserva restrita (antes `DEF-001`) | Baixa | P3 | Corrigido (`7d3c96b`) |
| [DEF-011](DEF-011-jwt-autoridades.md) | Autoridades dos perfis não extraídas do JWT (antes `DEF-002`) | Alta | P1 | Corrigido (`5639986`) |
| [DEF-012](DEF-012-perfil-perf-postgres-sem-credenciais.md) | Perfil `perf,postgres` não sobe sem credenciais por ambiente | Média | P2 | Mitigado por procedimento documentado |
| [DEF-013](DEF-013-concorrencia-it-ids-colidem-com-ciclo-de-vida.md) | `ReservationConcurrencyIT` falha por colisão de ids com o ciclo de vida | Média | P2 | Corrigido (`1793e76`) |
| [DEF-014](DEF-014-modo-em-memoria-permite-dupla-reserva.md) | Modo em memória permitia dupla reserva | Alta | P1 | Corrigido (`003867a`) |
| [DEF-015](DEF-015-estados-alternativos-ausentes.md) | Estados alternativos do ciclo de vida não implementados | Alta | P1 | Corrigido (`e264520`) |

`DEF-010` e `DEF-011` foram renumerados porque `DEF-001` e `DEF-002` estavam duplicados (os dois primeiros defeitos de concorrência e os dois primeiros da matriz de autorização). As mensagens de commit antigas ainda citam os IDs originais.
