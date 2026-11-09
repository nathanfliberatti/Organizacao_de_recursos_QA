import { api, session, setUnauthorizedHandler } from './api.js';
import { html, toMarkup, toast } from './util.js';
import * as login from './views/login.js';
import * as resources from './views/resources.js';
import * as reservations from './views/reservations.js';
import * as maintenance from './views/maintenance.js';
import * as loans from './views/loans.js';
import * as notifications from './views/notifications.js';
import * as report from './views/report.js';
import * as users from './views/users.js';

const ROUTES = [
  { path: 'recursos', label: 'Recursos', view: resources, roles: null },
  { path: 'reservas', label: 'Reservas', view: reservations, roles: null },
  { path: 'manutencao', label: 'Manutenção', view: maintenance, roles: null },
  { path: 'materiais', label: 'Materiais', view: loans, roles: ['RESPONSAVEL', 'ADMINISTRADOR'] },
  { path: 'notificacoes', label: 'Notificações', view: notifications, roles: null },
  { path: 'relatorio', label: 'Relatório', view: report, roles: ['RESPONSAVEL', 'ADMINISTRADOR'] },
  { path: 'usuarios', label: 'Usuários', view: users, roles: ['ADMINISTRADOR'] },
];
const ROLE_LABEL = { SOLICITANTE: 'Solicitante', RESPONSAVEL: 'Responsável', ADMINISTRADOR: 'Administrador' };

const main = document.getElementById('main');
const topbar = document.getElementById('topbar');
const nav = document.getElementById('nav');

const allowed = (route, user) => !route.roles || route.roles.includes(user.role);
export const navigate = (path) => { window.location.hash = `#/${path}`; };

function renderChrome(user, current) {
  topbar.hidden = false;
  nav.innerHTML = toMarkup(ROUTES.filter((r) => allowed(r, user)).map((r) =>
    html`<a href="#/${r.path}" ${r.path === current ? html`aria-current="page"` : ''}>${r.label}</a>`));
  document.getElementById('session').innerHTML = toMarkup(html`
    <span>${user.displayName} · ${ROLE_LABEL[user.role]}</span>
    <button class="secondary small" id="logout">Sair</button>`);
  document.getElementById('logout').addEventListener('click', logout);
}

function logout() {
  session.clear();
  topbar.hidden = true;
  navigate('login');
  route();
}

async function route() {
  const user = session.user;
  const current = (window.location.hash.replace(/^#\/?/, '') || '').split('?')[0];
  nav.classList.remove('open');
  document.getElementById('menu-toggle').setAttribute('aria-expanded', 'false');
  if (!user) {
    topbar.hidden = true;
    login.render(main, { onLogin: () => { navigate('recursos'); route(); } });
    return;
  }
  const target = ROUTES.find((r) => r.path === current && allowed(r, user)) || ROUTES[0];
  if (target.path !== current) { navigate(target.path); return; }
  renderChrome(user, target.path);
  main.innerHTML = '<p class="empty">Carregando…</p>';
  try {
    await target.view.render(main, { user, navigate });
  } catch (error) {
    main.innerHTML = toMarkup(html`<div class="card"><p class="error-text" role="alert">${error.message}</p></div>`);
  }
  main.focus({ preventScroll: true });
}

setUnauthorizedHandler(() => { toast('Sessão expirada. Entre novamente.', 'error'); logout(); });
document.getElementById('menu-toggle').addEventListener('click', (event) => {
  const open = nav.classList.toggle('open');
  event.currentTarget.setAttribute('aria-expanded', String(open));
});
window.addEventListener('hashchange', route);

(async () => {
  if (session.token) {
    try { const me = (await api('/api/auth/me')).data; session.save(session.token, me); } catch { session.clear(); }
  }
  route();
})();
