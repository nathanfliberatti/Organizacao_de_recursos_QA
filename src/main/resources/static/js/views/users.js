import { get, post } from '../api.js';
import { html, toMarkup, toast } from '../util.js';

const ROLES = { SOLICITANTE: 'Solicitante', RESPONSAVEL: 'Responsável', ADMINISTRADOR: 'Administrador' };

export async function render(container) {
  container.innerHTML = toMarkup(html`
    <h1>Usuários</h1>
    <section class="card" aria-labelledby="u-title">
      <h2 id="u-title">Novo usuário</h2>
      <form id="user-form" class="grid">
        <label>Usuário <input name="username" required></label>
        <label>Nome <input name="displayName" required></label>
        <label>Senha (mín. 8 caracteres) <input name="password" type="password" minlength="8" required autocomplete="new-password"></label>
        <label>Perfil <select name="role">${Object.entries(ROLES).map(([v, l]) => html`<option value="${v}">${l}</option>`)}</select></label>
        <button type="submit">Criar</button>
      </form>
    </section>
    <section class="card"><div id="list" class="table-wrap"></div></section>`);
  const list = container.querySelector('#list');
  async function load() {
    const users = await get('/api/users');
    list.innerHTML = toMarkup(html`<table><thead><tr><th>Usuário</th><th>Nome</th><th>Perfil</th></tr></thead>
      <tbody>${users.map((u) => html`<tr><td>${u.username}</td><td>${u.displayName}</td><td>${ROLES[u.role]}</td></tr>`)}</tbody></table>`);
  }
  container.querySelector('#user-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      await post('/api/users', Object.fromEntries(new FormData(event.target)));
      toast('Usuário criado.', 'ok'); event.target.reset(); load();
    } catch (error) { toast(error.status === 409 ? 'Usuário já existe.' : error.message, 'error'); }
  });
  await load();
}
