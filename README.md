# Organização de Recursos QA

## Build e testes

O projeto usa Maven Wrapper e requer Java 21.

```bash
./mvnw -B verify
```

Testes unitários JUnit 5 são executados pelo Surefire. Testes de integração com
sufixo `IT` são executados pelo Failsafe durante `verify`.

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

Variáveis do perfil `postgres`: `DB_URL`, `DB_USER`, `DB_PASSWORD` (obrigatória), `JWT_SECRET`, `APP_DEMO_DATA`,
`APP_DEMO_PASSWORD`.

Sem o perfil `postgres` a aplicação sobe com armazenamento em memória, apenas para desenvolvimento: **esse modo
não impede dupla reserva** (a garantia vem das constraints do PostgreSQL, ver ADR-001) e o relatório de
utilização não está disponível.

Detalhes de telas, endpoints e modelo de dados em [docs/frontend-e-banco-de-dados.md](docs/frontend-e-banco-de-dados.md).

## Integração contínua

O workflow em `.github/workflows/ci.yml` executa `./mvnw -B verify` em pull
requests e em pushes para `main`. Os relatórios Surefire/Failsafe são publicados
como artifact da execução quando existirem.

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
