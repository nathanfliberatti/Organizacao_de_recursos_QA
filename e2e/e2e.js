// Teste E2E da interface (Playwright). Não roda no CI: exige a aplicação no ar. Ver e2e/README.md.
const { chromium } = require('playwright');
const fs = require('fs');

const BASE = process.env.BASE_URL || 'http://127.0.0.1:8080';
const PASSWORD = process.env.E2E_PASSWORD;
const OUT = process.env.E2E_OUT || 'e2e-out';
const EXPECT_CONFLICT = process.env.E2E_EXPECT_CONFLICT === 'true'; // true somente com o perfil postgres
if (!PASSWORD) { console.error('Defina E2E_PASSWORD (senha dos usuários de demonstração).'); process.exit(2); }
fs.mkdirSync(OUT, { recursive: true });

const results = [];
const check = (name, ok, detail = '') => { results.push({ name, ok: !!ok, detail }); console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ' — ' + detail : ''}`); };
const tag = Math.random().toString(36).slice(2, 8);
const futureDay = () => 10 + Math.floor(Math.random() * 13); // dia 10 a 22; evita colisão entre execuções e deixa folga para +4 dias
const day = String(futureDay()).padStart(2, '0');
const month = String(1 + Math.floor(Math.random() * 12)).padStart(2, '0');
const year = 2030 + Math.floor(Math.random() * 60);
const date = `${year}-${month}-${day}`;

async function open(browser, viewport) {
  const ctx = await browser.newContext({ viewport: viewport || { width: 1200, height: 800 } });
  const page = await ctx.newPage();
  page.pageErrors = [];
  page.on('pageerror', (e) => page.pageErrors.push(e.message));
  page.on('dialog', (d) => d.accept()); // confirmações de Cancelar, Rejeitar e Não compareceu
  await page.goto(BASE + '/');
  return page;
}
async function login(page, user, password = PASSWORD) {
  await page.fill('input[name=username]', user);
  await page.fill('input[name=password]', password);
  await page.click('button[type=submit]');
}
const navItems = (page) => page.$$eval('#nav a', (a) => a.map((x) => x.textContent.trim()));
// Limpa o aviso anterior, executa a ação e devolve o texto do aviso novo (evita ler um aviso antigo).
const expectToast = async (page, action) => {
  await page.evaluate(() => { const t = document.getElementById('toast'); t.className = 'toast'; t.textContent = ''; });
  await action();
  await page.waitForFunction(() => document.getElementById('toast').classList.contains('show') && document.getElementById('toast').textContent.length > 0);
  return (await page.textContent('#toast')).trim();
};
const goTo = async (page, label, heading) => { await page.click(`nav a:has-text("${label}")`); await page.waitForSelector(`main h1:has-text("${heading}")`); };

(async () => {
  const browser = await chromium.launch();
  try {
    // ---- Login e perfis (RF-01)
    let p = await open(browser);
    check('tela de login mostra o formulário', await p.isVisible('#login-form'));
    check('barra de navegação oculta antes do login', !(await p.isVisible('#topbar')));
    await login(p, 'admin', 'senha-errada');
    await p.waitForSelector('#login-error:not([hidden])');
    check('credenciais inválidas mostram mensagem compreensível', (await p.textContent('#login-error')).includes('inválidos'));
    await login(p, 'admin');
    await p.waitForSelector('#nav a');
    const admin = await navItems(p);
    check('Administrador vê Usuários e Materiais', admin.includes('Usuários') && admin.includes('Materiais'), admin.join(', '));

    // ---- Cadastro e pesquisa de recursos (RF-02, RF-03), texto escapado
    const resId = `e2e-sala-${tag}`;
    await p.click('#new-resource');
    await p.fill('#resource-form [name=id]', resId);
    await p.fill('#resource-form [name=name]', `Sala <b>E2E</b> ${tag}`);
    await p.fill('#resource-form [name=capacity]', '8');
    await p.fill('#resource-form [name=location]', `Bloco ${tag}`);
    await p.check('#resource-form [name=restricted]');
    await p.click('#resource-form button[type=submit]');
    await p.waitForSelector(`td:has-text("${resId}")`);
    check('recurso cadastrado aparece na lista', true);
    check('HTML no nome é exibido como texto (sem injeção)', await p.isVisible('td:has-text("<b>E2E</b>")'));
    await p.fill('#search [name=location]', `bloco ${tag}`);
    await p.click('#search button[type=submit]');
    await p.waitForFunction(() => document.querySelectorAll('#results tbody tr').length === 1);
    check('pesquisa por localização retorna só o recurso criado', (await p.$$eval('#results tbody tr', (r) => r.length)) === 1);
    await p.screenshot({ path: `${OUT}/e2e-recursos-admin.png` });

    // ---- Manutenção (RF-08)
    await p.click('nav a:has-text("Manutenção")');
    await p.waitForSelector('#block');
    await p.selectOption('#block [name=resourceId]', resId);
    await p.fill('#block [name=startsAt]', `${date}T08:00`);
    await p.fill('#block [name=endsAt]', `${date}T18:00`);
    await p.click('#block button');
    await p.waitForSelector(`td:has-text("${resId}")`);
    check('período de manutenção listado', true);

    // ---- Solicitante: autorização na interface e reserva (RF-04, RF-07, RF-13)
    const s = await open(browser);
    await login(s, 'solicitante');
    await s.waitForSelector('#nav a');
    const solic = await navItems(s);
    check('Solicitante não vê Usuários, Materiais nem Relatório', !solic.some((i) => ['Usuários', 'Materiais', 'Relatório'].includes(i)), solic.join(', '));
    await s.goto(BASE + '/#/usuarios');
    await s.waitForTimeout(400);
    check('Solicitante é redirecionado ao abrir rota de Administrador', s.url().endsWith('#/recursos'), s.url());

    await s.click('nav a:has-text("Reservas")');
    await s.waitForSelector('#reserve');
    await s.selectOption('#reserve [name=resourceId]', resId);
    await s.fill('#reserve [name=startsAt]', `${date}T10:00`);
    await s.fill('#reserve [name=endsAt]', `${date}T09:00`);
    check('término antes do início é recusado com mensagem', (await expectToast(s, () => s.click('#reserve button[type=submit]'))).includes('posterior'));

    await s.fill('#reserve [name=startsAt]', `${date}T09:00`);
    await s.fill('#reserve [name=endsAt]', `${date}T10:00`);
    check('reserva durante a manutenção é recusada', (await expectToast(s, () => s.click('#reserve button[type=submit]'))).includes('indisponível'));

    const free = `${year}-${month}-${String(Number(day) + 1).padStart(2, '0')}`;
    await s.fill('#reserve [name=startsAt]', `${free}T09:00`);
    await s.fill('#reserve [name=endsAt]', `${free}T10:00`);
    await s.click('#reserve button[type=submit]');
    await s.waitForSelector(`tr:has-text("${tag}") .badge.SOLICITADA`);
    check('reserva de recurso restrito nasce SOLICITADA', true);
    check('Solicitante não vê o botão Aprovar', (await s.$$('button[data-action=approve]')).length === 0);
    if (EXPECT_CONFLICT) {
      check('mesmo horário é recusado por conflito (PostgreSQL)', (await expectToast(s, () => s.click('#reserve button[type=submit]'))).includes('indisponível'));
    }
    await s.screenshot({ path: `${OUT}/e2e-reservas-solicitante.png` });

    // ---- Responsável aprova e consulta histórico (RF-07, RF-10)
    const r = await open(browser);
    await login(r, 'responsavel');
    await r.waitForSelector('#nav a');
    check('Responsável vê Materiais e Relatório', (await navItems(r)).includes('Materiais') && (await navItems(r)).includes('Relatório'));
    await r.click('nav a:has-text("Reservas")');
    await r.waitForSelector(`tr:has-text("${tag}") button[data-action=approve]`);
    await r.click(`tr:has-text("${tag}") button[data-action=approve]`);
    await r.waitForSelector(`tr:has-text("${tag}") .badge.APROVADA`);
    check('aprovação muda a reserva para APROVADA', true);
    await r.click(`tr:has-text("${tag}") button[data-action=history]`);
    await r.waitForSelector('ol.timeline li');
    check('histórico registra criação e aprovação', (await r.$$eval('ol.timeline li', (l) => l.length)) >= 2);
    await r.click('[data-close]');

    // ---- Solicitante inicia o uso; cancelar deixa de ser permitido (RF-14, RF-15)
    await goTo(s, 'Recursos', 'Recursos');
    await goTo(s, 'Reservas', 'Reservas');
    await s.waitForSelector(`tr:has-text("${tag}") button[data-action=start]`);
    await s.click(`tr:has-text("${tag}") button[data-action=start]`);
    await s.waitForSelector(`tr:has-text("${tag}") .badge.EM_USO`);
    check('iniciar uso muda a reserva para EM USO', true);
    check('reserva EM USO não oferece cancelar', (await s.$$(`tr:has-text("${tag}") button[data-action=cancel]`)).length === 0);

    // ---- Notificações simuladas (RF-11)
    await goTo(s, 'Notificações', 'Notificações');
    await s.waitForSelector('#list table');
    check('notificação de criação registrada', (await s.textContent('#list')).includes('Criação'));

    // ---- Estados alternativos (RF-14): rejeitar, não comparecer e cancelar
    const dayAt = (offset) => `${year}-${month}-${String(Number(day) + offset).padStart(2, '0')}`;
    const reserveAt = async (offset) => {
      await goTo(s, 'Recursos', 'Recursos');
      await goTo(s, 'Reservas', 'Reservas');
      await s.selectOption('#reserve [name=resourceId]', resId);
      await s.fill('#reserve [name=startsAt]', `${dayAt(offset)}T09:00`);
      await s.fill('#reserve [name=endsAt]', `${dayAt(offset)}T10:00`);
      const text = await expectToast(s, () => s.click('#reserve button[type=submit]'));
      const match = text.match(/Reserva (\d+) criada/);
      return match ? match[1] : null;
    };
    const rowOf = (page, id) => page.locator(`tr:has(td:first-child:text-is("${id}"))`);
    const refresh = async (page) => { await goTo(page, 'Recursos', 'Recursos'); await goTo(page, 'Reservas', 'Reservas'); };
    const actionsIn = async (page, id) => page.$$eval(`tr:has(td:first-child:text-is("${id}")) button`, (b) => b.map((x) => x.textContent.trim()));

    const rejectedId = await reserveAt(2);
    check('nova solicitação de recurso restrito criada', !!rejectedId);
    await refresh(r);
    await rowOf(r, rejectedId).locator('button[data-action=reject]').click();
    await r.waitForSelector(`tr:has(td:first-child:text-is("${rejectedId}")) .badge.REJEITADA`);
    check('Responsável rejeita e a reserva vira REJEITADA', true);
    const afterReject = await actionsIn(r, rejectedId);
    check('reserva REJEITADA só oferece Histórico', afterReject.length === 1 && afterReject[0] === 'Histórico', afterReject.join(', '));

    const noShowId = await reserveAt(3);
    await refresh(r);
    await rowOf(r, noShowId).locator('button[data-action=approve]').click();
    await r.waitForSelector(`tr:has(td:first-child:text-is("${noShowId}")) .badge.APROVADA`);
    await rowOf(r, noShowId).locator('button[data-action=noshow]').click();
    await r.waitForSelector(`tr:has(td:first-child:text-is("${noShowId}")) .badge.NAO_COMPARECEU`);
    check('Responsável registra não comparecimento e a reserva vira NÃO COMPARECEU', true);
    const afterNoShow = await actionsIn(r, noShowId);
    check('reserva NÃO COMPARECEU só oferece Histórico', afterNoShow.length === 1 && afterNoShow[0] === 'Histórico', afterNoShow.join(', '));

    const cancelId = await reserveAt(4);
    await refresh(s);
    await rowOf(s, cancelId).locator('button[data-action=cancel]').click();
    await s.waitForSelector(`tr:has(td:first-child:text-is("${cancelId}")) .badge.CANCELADA`);
    check('Solicitante cancela e a reserva vira CANCELADA', true);
    check('reserva CANCELADA não oferece ações ao Solicitante', (await actionsIn(s, cancelId)).length === 0);
    const again = await reserveAt(4);
    check('horário liberado pelo cancelamento pode ser reservado de novo', !!again && again !== cancelId);
    await refresh(r);
    await rowOf(r, cancelId).locator('button[data-action=history]').click();
    await r.waitForSelector('ol.timeline li');
    check('histórico da reserva cancelada registra a transição para CANCELADA', (await r.textContent('ol.timeline')).includes('CANCELADA'));
    await r.click('[data-close]');
    await refresh(r);
    await r.screenshot({ path: `${OUT}/e2e-estados-alternativos.png`, fullPage: true });

    // ---- Responsividade (RNF-04)
    const m = await open(browser, { width: 375, height: 700 });
    await login(m, 'solicitante');
    await m.waitForSelector('#menu-toggle');
    check('menu recolhido em tela estreita', !(await m.isVisible('#nav')));
    await m.click('#menu-toggle');
    check('menu abre ao tocar em Menu', await m.isVisible('#nav'));
    await m.screenshot({ path: `${OUT}/e2e-mobile.png` });

    // ---- Sessão
    await r.click('#logout');
    await r.waitForSelector('#login-form');
    check('sair volta ao login', true);
    for (const page of [p, s, r, m]) check('sem erros de JavaScript não tratados', page.pageErrors.length === 0, page.pageErrors.join(' | '));
  } catch (e) {
    check('execução sem exceção', false, e.message.split('\n')[0]);
  } finally {
    await browser.close();
  }
  const failed = results.filter((x) => !x.ok);
  console.log(`\n${results.length - failed.length}/${results.length} verificações passaram`);
  process.exit(failed.length ? 1 : 0);
})();
