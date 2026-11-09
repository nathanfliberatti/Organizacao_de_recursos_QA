import { api, session } from '../api.js';
import { html, toMarkup } from '../util.js';

export function render(container, { onLogin }) {
  container.innerHTML = toMarkup(html`
    <section class="card login" aria-labelledby="login-title">
      <h1 id="login-title">Entrar</h1>
      <form id="login-form" class="grid" style="grid-template-columns:1fr">
        <label>Usuário <input name="username" autocomplete="username" required autofocus></label>
        <label>Senha <input name="password" type="password" autocomplete="current-password" required></label>
        <p id="login-error" class="error-text" role="alert" hidden></p>
        <button type="submit">Entrar</button>
      </form>
    </section>`);
  const form = container.querySelector('#login-form');
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorNode = container.querySelector('#login-error');
    errorNode.hidden = true;
    const data = Object.fromEntries(new FormData(form));
    try {
      const { data: result } = await api('/api/auth/login', { method: 'POST', body: data });
      session.save(result.access_token, result.user);
      onLogin();
    } catch (error) {
      errorNode.textContent = error.status === 401 ? 'Usuário ou senha inválidos.' : error.message;
      errorNode.hidden = false;
    }
  });
}
