# Organização de Recursos QA

## Build e testes

O projeto usa Maven Wrapper e requer Java 21.

```bash
./mvnw -B verify
```

Testes unitários JUnit 5 são executados pelo Surefire. Testes de integração com
sufixo `IT` são executados pelo Failsafe durante `verify`.

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
