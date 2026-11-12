# Organização de Recursos QA

## Pré-requisitos

| Para | Precisa de |
|---|---|
| Compilar e rodar os testes unitários (`./mvnw -B test`) | Java 21 (o Maven Wrapper baixa o Maven) |
| `./mvnw -B verify` completo (inclui os `*IT`) | Java 21 **e Docker em execução**: os `*IT` sobem um PostgreSQL 16 descartável com Testcontainers. Sem Docker eles falham com `Could not find a valid Docker environment`. |
| Rodar a aplicação com banco | Docker (`docker compose`) **ou** um PostgreSQL 16 instalado (usuário com permissão para `CREATE EXTENSION btree_gist`) |
| Rodar a aplicação sem banco (somente demonstração) | Java 21 |

## Build e testes

```bash
./mvnw -B verify
```

Testes unitários JUnit 5 são executados pelo Surefire. Testes de integração com
sufixo `IT` são executados pelo Failsafe durante `verify` (exigem Docker).
Relatório de cobertura (JaCoCo): `target/site/jacoco/index.html`; a execução falha abaixo de 80% de linhas.

## Executando a aplicação (frontend + API + banco)

O frontend é estático (HTML/CSS/JS, sem build) e é servido pelo próprio Spring Boot em `/`.

```bash
export DB_PASSWORD='<senha-do-banco>'
export JWT_SECRET='<segredo-com-pelo-menos-32-caracteres>'
export APP_DEMO_PASSWORD='<senha-dos-usuarios-de-demonstracao>'
docker compose up -d db
APP_DEMO_DATA=true SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

Abra <http://localhost:8080>. Com `APP_DEMO_DATA=true`, e somente se a tabela `app_users` estiver vazia, são
criados os usuários de demonstração `admin`, `responsavel` e `solicitante` e alguns recursos. A senha deles é a de
`APP_DEMO_PASSWORD`; se não for definida, uma senha aleatória é gerada e aparece no log de inicialização.
Nenhum segredo fica no repositório. Sem `JWT_SECRET`, a chave do JWT é aleatória a cada execução (tokens expiram ao
reiniciar). Não use dados de demonstração em produção.

Notificações: por padrão a entrega é simulada (registro em `/api/notifications`). Para integrar uma API externa, defina `NOTIFICATION_WEBHOOK_URL` (http ou https); cada evento de reserva vira um `POST` JSON nessa URL, e uma falha dela não afeta a reserva.

Variáveis do perfil `postgres`: `DB_URL`, `DB_USER`, `DB_PASSWORD` (obrigatória), `JWT_SECRET`, `APP_DEMO_DATA`,
`APP_DEMO_PASSWORD`.

Sem o perfil `postgres` a aplicação sobe com armazenamento em memória, apenas para desenvolvimento: ele **também
impede dupla reserva** (sobreposição de sala e de professor, de forma atômica) dentro de uma instância, mas os dados
somem ao parar, a garantia entre várias instâncias vem das constraints do PostgreSQL (ver ADR-001) e o relatório de
utilização não está disponível.

Sem Docker e sem PostgreSQL, para ver as telas e experimentar os fluxos (os dados somem ao parar):

```bash
APP_DEMO_DATA=true APP_DEMO_PASSWORD='<senha>' ./mvnw spring-boot:run
```

No PowerShell use `$env:APP_DEMO_DATA="true"; $env:APP_DEMO_PASSWORD="<senha>"; .\mvnw spring-boot:run`.

## Documentação

- **API (endpoints, perfis, corpos e códigos):** [docs/api.md](docs/api.md)
- Telas, modelo de dados e lacunas: [docs/frontend-e-banco-de-dados.md](docs/frontend-e-banco-de-dados.md)
- Arquitetura do sistema entregue (C4 e sequências): [docs/arquitetura-implementada.md](docs/arquitetura-implementada.md) · Desenho original: [docs/arquitetura.md](docs/arquitetura.md) · Requisitos: [docs/requisitos-funcionais.md](docs/requisitos-funcionais.md), [docs/requisitos-nao-funcionais.md](docs/requisitos-nao-funcionais.md)
- Rastreabilidade: [docs/rtm.md](docs/rtm.md) · Defeitos: [docs/defeitos/](docs/defeitos/README.md) · Testes: [docs/testes/relatorio-testes.md](docs/testes/relatorio-testes.md) (plano em [plano-tdd.md](docs/testes/plano-tdd.md)) · E2E da interface: [e2e/README.md](e2e/README.md)
- Desempenho: [docs/performance/](docs/performance/rnf.md) · Qualidade: [docs/relatorio-qualidade.md](docs/relatorio-qualidade.md), [docs/decisao-qualidade.md](docs/decisao-qualidade.md) · Evidências: [docs/evidencias/README.md](docs/evidencias/README.md) · Entrega: [docs/entrega/checklist.md](docs/entrega/checklist.md)

## Integração contínua

O workflow em `.github/workflows/ci.yml` executa `./mvnw -B verify` em pull
requests e em pushes para `main`. Os relatórios Surefire/Failsafe são publicados
como artifact da execução quando existirem; o relatório do JaCoCo também é publicado como artifact
(`jacoco-report`, retido por 30 dias). O SonarCloud analisa cada pull request e o Quality Gate bloqueia o merge.

## Relatório de utilização

No perfil `postgres`, o endpoint
`GET /relatorios/utilizacao?inicio=2026-11-02T10:00:00Z&fim=2026-11-02T12:00:00Z`
retorna horas reservadas por recurso, carga horária por professor e a quantidade
de tentativas recusadas por sobreposição. `inicio` e `fim` são instantes ISO-8601
e delimitam o intervalo semiaberto `[inicio, fim)`. Reservas canceladas não contam
como uso; reservas que cruzam os limites são contabilizadas somente pelo trecho
dentro do período. Uma tentativa recusada conta quando seu intervalo se sobrepõe
ao período solicitado.

O acesso exige autenticação e os perfis `RESPONSAVEL` ou `ADMINISTRADOR`.
Solicitações sem autenticação recebem `401`; o perfil `SOLICITANTE` recebe `403`.
O endpoint retorna `400` quando `inicio` não precede `fim`.

Exemplo de resposta:

```json
{
  "inicio": "2026-11-02T10:00:00Z",
  "fim": "2026-11-02T12:00:00Z",
  "horasPorRecurso": [
    { "recursoId": "room-1", "horasReservadas": 1.0 }
  ],
  "cargaPorProfessor": [
    { "professorId": "professor-1", "horasAlocadas": 1.0 }
  ],
  "conflitosEvitados": 2
}
```
