# language: pt
Funcionalidade: Estados alternativos da reserva (RF-14)
  Como Responsável, Administrador ou Solicitante
  Quero rejeitar, cancelar e registrar o não comparecimento de reservas
  Para que cada mudança de estado seja feita só por quem tem permissão e fique auditada

  Cenário: RF-14 — Responsável rejeita uma solicitação
    Dado que o solicitante "solicitante-1" criou uma solicitação de recurso restrito
    Quando o usuário "responsavel-1" com perfil "RESPONSAVEL" rejeita a reserva
    Então a reserva deve estar no estado "REJEITADA"
    E a auditoria deve registrar a transição de "SOLICITADA" para "REJEITADA" feita por "responsavel-1"

  Cenário: RF-14 — Solicitante não pode rejeitar uma solicitação
    Dado que o solicitante "solicitante-1" criou uma solicitação de recurso restrito
    Quando o usuário "solicitante-2" com perfil "SOLICITANTE" rejeita a reserva
    Então a resposta deve ter status 403
    E a reserva deve estar no estado "SOLICITADA"

  Cenário: RF-14 — Responsável não rejeita a própria solicitação
    Dado que o solicitante "responsavel-1" criou uma solicitação de recurso restrito
    Quando o usuário "responsavel-1" com perfil "RESPONSAVEL" rejeita a reserva
    Então a reserva deve estar no estado "SOLICITADA"

  Cenário: RF-14 — Solicitante cancela a própria reserva antes de usá-la
    Dado que o solicitante "solicitante-1" criou uma solicitação de recurso restrito
    Quando o usuário "solicitante-1" com perfil "SOLICITANTE" cancela a reserva
    Então a resposta deve ter status 204
    E a reserva deve estar no estado "CANCELADA"
    E a auditoria deve registrar a transição de "SOLICITADA" para "CANCELADA" feita por "solicitante-1"

  Cenário: RF-14 — Outro solicitante não cancela a reserva alheia
    Dado que o solicitante "solicitante-1" criou uma solicitação de recurso restrito
    Quando o usuário "solicitante-2" com perfil "SOLICITANTE" cancela a reserva
    Então a resposta deve ter status 403
    E a reserva deve estar no estado "SOLICITADA"

  Cenário: RF-15 — Reserva iniciada não pode ser cancelada
    Dado que a reserva do solicitante "solicitante-1" foi aprovada e iniciada
    Quando o usuário "solicitante-1" com perfil "SOLICITANTE" cancela a reserva
    Então a resposta deve ter status 409
    E a reserva deve estar no estado "EM_USO"

  Cenário: RF-14 — Responsável registra o não comparecimento de uma reserva aprovada
    Dado que a reserva do solicitante "solicitante-1" foi aprovada
    Quando o usuário "responsavel-1" com perfil "RESPONSAVEL" registra o não comparecimento
    Então a reserva deve estar no estado "NAO_COMPARECEU"
    E a auditoria deve registrar a transição de "APROVADA" para "NAO_COMPARECEU" feita por "responsavel-1"

  Cenário: RF-14 — Solicitante não registra o não comparecimento
    Dado que a reserva do solicitante "solicitante-1" foi aprovada
    Quando o usuário "solicitante-1" com perfil "SOLICITANTE" registra o não comparecimento
    Então a resposta deve ter status 403
    E a reserva deve estar no estado "APROVADA"

  Cenário: RF-14 — Estado final não aceita nova transição
    Dado que o solicitante "solicitante-1" criou uma solicitação de recurso restrito
    E que o usuário "responsavel-1" com perfil "RESPONSAVEL" rejeitou a reserva
    Quando o usuário "responsavel-1" com perfil "RESPONSAVEL" tenta aprovar a reserva
    Então a reserva deve estar no estado "REJEITADA"
