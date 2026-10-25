# language: pt
Funcionalidade: Aprovação, manutenção e ciclo de vida de reservas
  Como usuário do sistema de Organização de Recursos
  Quero consultar e operar reservas pela API
  Para garantir aprovação autorizada, indisponibilidade e auditoria

  Cenário: RF-07 — Aprovação antes de considerar aprovada
    Dado que uma reserva solicitada utiliza um recurso classificado como restrito
    E que a decisão de um Responsável ainda não foi registrada
    Quando o sistema consulta o estado da reserva
    Então a reserva não deve ser tratada como aprovada

  Cenário: RF-07 — Decisão por Responsável
    Dado que existe uma solicitação pendente para um recurso restrito
    Quando um usuário com perfil Responsável aprova a solicitação
    Então a decisão deve ser registrada
    E a reserva deve refletir a aprovação
    E a mudança de estado deve gerar auditoria com ação, reserva, usuário e data/hora, conforme RF-10

  Cenário: API — Impedir autoaprovação
    Dado que a reserva restrita foi criada pela identidade autenticada
    E que essa identidade possui perfil Responsável
    Quando ela tenta aprovar a própria reserva pela API
    Então a resposta não deve indicar aprovação
    E o estado deve permanecer "SOLICITADA"
    E a decisão deve permanecer ausente

  Cenário: RF-07 — Solicitante não pode aprovar solicitação restrita
    Dado que uma reserva restrita aguarda decisão e o usuário autenticado tem perfil Solicitante
    E que sua identidade é diferente da identidade que criou a reserva
    Quando esse usuário tenta aprovar a solicitação
    Então a operação deve ser negada
    E o estado e a decisão da reserva devem permanecer inalterados

  Cenário: RF-08 — Bloquear reserva durante manutenção
    Dado que um recurso possui período de manutenção registrado
    E que o período solicitado para reserva contém tempo dentro da manutenção
    Quando o Solicitante tenta reservar o recurso
    Então a reserva deve ser recusada
    E o recurso deve ser tratado como indisponível para esse período

  Cenário: API — Fluxo principal e auditoria
    Dado que uma reserva restrita foi solicitada e aguarda decisão
    Quando um Responsável distinto aprova, inicia e conclui a reserva pelas operações públicas
    Então os estados observados devem seguir "APROVADA", "EM_USO" e "CONCLUÍDA"
    E a resposta final deve expor os três registros de transição com ator e instante
