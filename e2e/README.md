# Teste E2E da interface (Playwright)

Verifica os fluxos principais no navegador (Chromium): login e perfis, cadastro/pesquisa de recursos, manutenção, reservas com aprovação, histórico, notificações, responsividade e logout. **Não roda no CI**: exige a aplicação no ar e uma instalação do Playwright. Resultado de uma execução: [docs/testes/e2e-resultado.md](../docs/testes/e2e-resultado.md).

## Como executar

1. Suba a aplicação com usuários de demonstração (com o perfil `postgres`; sem o perfil também funciona, porque o modo em memória recusa conflito de horário):

   ```bash
   export DB_PASSWORD='<senha-do-banco>' APP_DEMO_DATA=true APP_DEMO_PASSWORD='<senha-demo>'
   SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
   ```

2. Em outro terminal:

   ```bash
   cd e2e
   npm install playwright   # uma vez; baixa o Chromium com: npx playwright install chromium
   E2E_PASSWORD='<senha-demo>' E2E_EXPECT_CONFLICT=true node e2e.js   # 37 verificações
   ```

Variáveis: `E2E_PASSWORD` (obrigatória), `BASE_URL` (padrão `http://127.0.0.1:8080`), `E2E_OUT` (pasta dos prints, padrão `e2e-out`), `E2E_EXPECT_CONFLICT` (`true` verifica que o mesmo horário é recusado; vale nos dois modos). O script cria recursos com sufixo aleatório e datas futuras aleatórias, então pode ser repetido. Código de saída `0` = tudo passou.
