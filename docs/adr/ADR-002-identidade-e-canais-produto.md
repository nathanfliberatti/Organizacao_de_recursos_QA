# ADR-002 — Identidade e canais do produto

- **Status:** Aceito
- **Data:** 2026-11-05
- **Contexto:** O produto será desenvolvido por stories até cobrir o escopo completo. É necessário definir a arquitetura de aplicação, a identidade dos usuários e uma ordem de entrega que cumpra o Artigo I (CLI First), sem deslocar regras de autorização para os clientes nem antecipar escolhas de interface ainda abertas.

## Decisão

Adotar um monólito Spring Boot, com a API como fonte única das regras de negócio e autorização. O produto completo continua sendo desenvolvido por stories; a CLI definida aqui é somente o primeiro slice, não uma declaração de conclusão dos requisitos.

### Ordem de entrega e canais

1. Implementar a API e a CLI antes da UI, para cumprir o Artigo I.
2. A CLI será um cliente HTTP escrito em Java 21, usando JDK `HttpClient`. Ela será incluída no JAR executável Maven existente e invocada como `java -jar <jar> cli ...`. Não terá acesso direto a stores nem substituirá a autenticação e autorização do servidor.
3. O primeiro slice da CLI cobre `users create Admin`; `resources list`, expondo somente os campos atuais `id` e `capacity`; `resources availability`; `reservations create`, somente para o próprio Solicitante; e `reservations list`, somente com reservas próprias. Esse slice não marca `RF-03` como completo.
4. Depois da CLI e da API, adicionar frontend estático same-origin em `src/main/resources/static/`. A escolha de framework JavaScript permanece em aberto. No primeiro momento, a aplicação web manterá o bearer token somente em memória; não usará `localStorage` nem refresh token sem decisão posterior.

### Identidade, credenciais e autorização

- Persistir usuários em PostgreSQL e armazenar senhas com hashes BCrypt. Não haverá cadastro público.
- O primeiro usuário `ADMINISTRADOR` só poderá ser inicializado se ainda não existir nenhum Administrador. O bootstrap usa exclusivamente `BOOTSTRAP_ADMIN_LOGIN` e `BOOTSTRAP_ADMIN_PASSWORD`, sem valores padrão, e falha fechado se qualquer uma estiver ausente.
- Administradores poderão criar usuários e atribuir papéis.
- `POST /api/auth/login` autentica e retorna JWT Bearer com validade máxima de 15 minutos. O segredo de assinatura do produto é configurado pela variável de ambiente `RECURSOS_JWT_SECRET`, fora do repositório e sem fallback ou valor padrão. A configuração é obrigatória para autenticação de produto: se estiver ausente, vazia ou tiver menos de 32 bytes, a inicialização da aplicação deve falhar, sem disponibilizar rotas protegidas. O mínimo de 32 bytes aplica-se à chave HS256. Não registrar o valor do segredo, credenciais ou tokens.
- O profile `perf` permanece isolado da autenticação de produto e usa sua própria configuração de segredo; não depende de `RECURSOS_JWT_SECRET` nem fornece fallback para o produto.
- Em cada request autenticado, verificar no banco se o usuário continua ativo e carregar seus papéis atuais. Desativação ou mudança de papel deve revogar o acesso correspondente sem depender da expiração do token.
- A CLI solicita a senha via stdin e não persiste token ou sessão. O login ocorre em cada invocação.
- Clientes não são fronteiras de segurança: a API sempre autentica e autoriza a operação no servidor.

### Identidade do solicitante e professor

`RF-04` e a persona definem o Solicitante como professor que reserva para si. Para não atribuir uma reserva a outro professor, o subject/login canônico do usuário com papel `SOLICITANTE` será o próprio `professorId`, que também será o `requesterId`; a criação de reserva não aceitará `professorId` alternativo fornecido pelo Solicitante.

Esta é uma decisão provisória e conservadora diante da ausência atual de uma tabela de professores. Deve ser revalidada quando o catálogo de professores for definido. A consulta de disponibilidade pode usar `professorId` conforme o contrato existente; esta decisão não amplia os dados expostos.

## Alternativas consideradas

- **Entregar a UI antes da CLI:** rejeitada porque contraria a ordem necessária para cumprir o Artigo I.
- **Implementar a CLI com acesso direto aos stores:** rejeitada; duplicaria ou contornaria regras e autorização que pertencem à API.
- **Manter API e frontend como serviços separados:** não adotada; o frontend será estático, same-origin, servido pelo Spring Boot, sem impedir uma revisão futura da decisão.
- **Cadastro público ou credenciais de bootstrap com defaults:** rejeitados; o bootstrap controlado pelo ambiente evita uma conta inicial implícita e o cadastro fica sob responsabilidade administrativa.
- **Confiar apenas no papel contido no JWT até sua expiração:** rejeitada; não atende à revogação imediata após desativação ou mudança de papel.
- **Persistir token/sessão na CLI ou usar armazenamento durável/refresh token na web desde o início:** não adotado; login por invocação na CLI e bearer somente em memória no primeiro momento limitam a persistência de credenciais no cliente.
- **Permitir que o Solicitante informe outro professor na reserva:** rejeitada; conflita com a definição de reserva própria em `RF-04`.

## Consequências

- A API concentra regras e autorização, enquanto CLI e futura UI atuam como clientes autenticados; a autorização permanece obrigatória no servidor em ambos os canais.
- O fluxo inicial exercita API e operações essenciais via CLI antes da interface web, mas não representa cobertura completa do produto nem conclusão de `RF-03`.
- Consultar usuário e papéis no banco em cada request autenticado permite refletir desativação e alteração de papel imediatamente, ao custo de leitura operacional adicional.
- O primeiro Administrador depende de configuração explícita e operacionalmente protegida; faltando qualquer variável de bootstrap quando ainda não há Administrador, a inicialização não deve prosseguir com credenciais padrão.
- A inicialização da autenticação de produto também depende de `RECURSOS_JWT_SECRET` válida; espera-se teste de bootstrap/configuração que confirme falha de inicialização quando a variável estiver ausente, vazia ou abaixo do mínimo de 32 bytes, sem disponibilizar rotas protegidas.
- O mapeamento `SOLICITANTE` ↔ `professorId` evita identidade de reserva escolhida pelo cliente, mas cria dependência provisória entre identidade e catálogo de professores.
- Framework JavaScript, contratos detalhados e campos de operações não cobertos pelo slice, política de refresh e aspectos operacionais não especificados permanecem fora desta decisão.

## Pendências

- Validar o mapeamento de identidade do Solicitante quando o catálogo de professores e sua relação com usuários forem definidos.
- Escolher framework JavaScript para o frontend estático.
- Definir contratos, payloads e cobertura dos demais fluxos por suas stories e requisitos, sem inferir regras de transição ou filtros nesta decisão.
- Definir se e quando haverá refresh token ou outro mecanismo de sessão web além do bearer em memória inicial.
- Definir o processo operacional de gestão, distribuição, rotação e resposta a comprometimento do segredo JWT, incluindo eventual gerenciador de segredos; a configuração externa obrigatória sem default e seu mínimo de tamanho estão definidos, mas o ciclo de vida da chave permanece pendente.

## Referências

- [Arquitetura do sistema](../arquitetura.md)
- [RF-04 e demais requisitos funcionais](../requisitos-funcionais.md)
- [Personas](../personas/)
- [ADR-001 — Integridade concorrente de reservas no PostgreSQL](ADR-001-integridade-concorrente-postgresql.md)