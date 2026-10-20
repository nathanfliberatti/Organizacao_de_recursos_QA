# Organização de Recursos QA

## Build e testes

O projeto usa Maven Wrapper e requer Java 21.

```bash
./mvnw -B verify
```

Testes unitários JUnit 5 são executados pelo Surefire. Testes de integração com
sufixo `IT` são executados pelo Failsafe durante `verify`.

O módulo Maven está inicializado, mas ainda não contém código de produção nem
testes de domínio. Um `verify` bem-sucedido neste estado valida somente o ciclo
de build; não é evidência de cobertura das regras listadas em
[`docs/testes/plano-tdd.md`](docs/testes/plano-tdd.md).

## Integração contínua

O workflow em `.github/workflows/ci.yml` executa `./mvnw -B verify` em pull
requests e em pushes para `main`. Os relatórios Surefire/Failsafe são publicados
como artifact da execução quando existirem.
