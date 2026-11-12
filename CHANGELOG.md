# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Versionamento: tags anotadas `vMAJOR.MINOR.PATCH[-rcN]`; uma tag publicada nunca é movida (correções geram `-rc2`, `-rc3`, …).

## [1.0.0-rc1] — 2026-11-12

Primeiro release candidate do sistema de Organização de Recursos.

### Adicionado
- **Frontend** SPA estática (HTML/CSS/JS, sem build) servida pelo Spring Boot: login, recursos, reservas, manutenção, materiais, notificações, relatório mensal e usuários, com navegação por perfil e layout responsivo.
- **Login real** com usuários no banco (`app_users`, hash BCrypt) e JWT HS256; gestão de usuários pelo Administrador.
- **Recursos** com tipo (`SALA`, `PROFESSOR`, `MATERIAL`), localização, competência e flag de recurso restrito; pesquisa por tipo, capacidade, localização, competência, texto e disponibilidade (RF-02, RF-03).
- **Reservas**: listagem, alteração e cancelamento pelo dono; ciclo de vida auditado ligado à agenda, com aprovação automática para recursos não restritos e aprovação do Responsável para restritos (RF-04, RF-07, RF-14).
- **Manutenção** persistida no banco, com listagem e remoção (RF-08).
- **Materiais**: retirada e devolução com no máximo uma retirada em aberto por material (RF-09).
- **Notificações** simuladas para criação, alteração e cancelamento de reserva (RF-11).
- **Estados alternativos do ciclo de vida (RF-14)**: `REJEITADA`, `CANCELADA` e `NAO_COMPARECEU`, com matriz de transições única, auditoria de cada mudança e horário devolvido à agenda; novas rotas `POST /api/reservations/{id}/rejection` e `/no-show`; cancelar passa a ser uma transição auditada.
- **Integração externa de notificação (RF-11)**: webhook opcional (`NOTIFICATION_WEBHOOK_URL`), testado com WireMock; a falha do webhook nunca derruba a reserva.
- Registro de riscos técnicos (`docs/riscos.md`) e coluna **Risco** na RTM; severidade e prioridade em todos os defeitos; apresentação e roteiro de demonstração (`docs/apresentacao/`).
- Banco: tabelas `app_users`, `maintenance_periods`, `material_loans`, `notifications` e novas colunas em `resources`; `docker-compose.yml` com PostgreSQL 16.
- Arquitetura do sistema implementado ([docs/arquitetura-implementada.md](docs/arquitetura-implementada.md)): C4 de contexto, contêineres e componentes e cinco sequências críticas, com imagens em `docs/diagramas/`.
- Relatório de testes consolidado ([docs/testes/relatorio-testes.md](docs/testes/relatorio-testes.md)) com o estado de cada exigência do RNF-07.
- Testes: `ReservationIntervalValidationTest` (parametrizado, RF-13), pesquisa por competência e texto livre (RF-03), `ApiDocumentationTest` (todo endpoint precisa constar em `docs/api.md`) e E2E da interface com Playwright (`e2e/`, fora do CI).
- Capturas do Sonar, JaCoCo, CI e aprovações em `docs/evidencias/imagens/`; defeitos `DEF-012` e `DEF-013`.
- Documentação: [docs/api.md](docs/api.md), [docs/frontend-e-banco-de-dados.md](docs/frontend-e-banco-de-dados.md), [docs/entrega/checklist.md](docs/entrega/checklist.md), [docs/defeitos/README.md](docs/defeitos/README.md), [docs/relatorio-qualidade.md](docs/relatorio-qualidade.md), [docs/evidencias/README.md](docs/evidencias/README.md), [docs/decisao-qualidade.md](docs/decisao-qualidade.md), resultado de desempenho `resultado-02`.

- **Matriz de autorização completa** (PR #14): 35 linhas × 4 perfis em `docs/seguranca/matriz-autorizacao.md`, verificadas célula a célula por `AuthorizationMatrixFullIT`; `AuthorizationMatrixDocumentTest` falha se uma rota mapeada ficar fora da matriz.
- **Mutação manual refeita** (PR #14, `docs/testes/mutacao-manual.md`): 21 mutantes, 19 mortos; três lacunas de teste fechadas (limites da manutenção, atomicidade da auditoria sem transação externa, cancelamento de reserva iniciada).
- **Relatório de qualidade no formato da Aula 13** (`docs/relatorio-qualidade.md`): evidência, interpretação e limite em cada afirmação.

### Alterado
- O perfil padrão agora sobe sem DataSource (armazenamento em memória, somente desenvolvimento); o perfil `postgres` exige `DB_PASSWORD`.
- Nenhum segredo fica no repositório: `JWT_SECRET` e `APP_DEMO_PASSWORD` opcionais (valores aleatórios por execução quando ausentes).
- CI: artifacts de teste e JaCoCo retidos por 30 dias (eram 3).
- Defeitos duplicados renumerados: `DEF-001` (criação restrita) → `DEF-010` e `DEF-002` (JWT) → `DEF-011`.
- RTM, status dos requisitos, relatório de testes e arquitetura atualizados.

### Corrigido
- **Dupla reserva no modo em memória** (DEF-014): o armazenamento em memória aceitava qualquer reserva; agora recusa sobreposição de sala e de professor, de forma atômica.
- Issues do SonarCloud no código novo: `SecureRandom` reutilizável e consultas SQL constantes.
- Isolamento do `ReservationConcurrencyIT` (ids reiniciados colidiam com o ciclo de vida).

### Limitações conhecidas
- Não há regra de tempo para registrar "não compareceu" (pode ser registrado antes do início da reserva); a matriz de transições adotada não foi validada com usuários reais.
- Fuso do fechamento mensal do relatório: UTC (decisão de negócio pendente).
- O modo em memória vale para uma única instância e perde os dados ao parar.
- Notificações são simuladas por padrão; o webhook opcional não reenvia em caso de falha.
- O job diário de treinamento descrito em `docs/arquitetura.md` não foi implementado.

## [anteriores]
Marcos já na `main` antes desta versão: CI com JaCoCo e SonarCloud, cenários Cucumber de aprovação, integridade concorrente no PostgreSQL (ADR-001), histórico auditável, relatório de utilização, matriz de autorização e harness de carga.
