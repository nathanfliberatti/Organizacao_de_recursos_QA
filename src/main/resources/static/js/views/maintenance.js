import { get, post, del } from '../api.js';
import { html, toMarkup, toIso, fmtDate, toast } from '../util.js';

export async function render(container, { user }) {
  const isAdmin = user.role === 'ADMINISTRADOR';
  const resources = (await get('/api/resources')).filter((r) => r.type !== 'PROFESSOR');
  container.innerHTML = toMarkup(html`
    <h1>Manutenção de recursos</h1>
    ${isAdmin ? html`<section class="card" aria-labelledby="m-title">
      <h2 id="m-title">Bloquear recurso</h2>
      <form id="block" class="grid">
        <label>Recurso <select name="resourceId" required><option value="">Selecione…</option>
          ${resources.map((r) => html`<option value="${r.id}">${r.name} (${r.id})</option>`)}</select></label>
        <label>Início <input name="startsAt" type="datetime-local" required></label>
        <label>Fim <input name="endsAt" type="datetime-local" required></label>
        <button type="submit">Bloquear</button>
      </form>
      <p class="hint">Durante o período o recurso fica indisponível e não pode ser reservado. Cada recurso tem um período de manutenção.</p>
    </section>` : ''}
    <section class="card"><div id="list" class="table-wrap"></div></section>`);
  const list = container.querySelector('#list');

  async function load() {
    const periods = await get('/api/maintenance');
    list.innerHTML = toMarkup(periods.length === 0 ? html`<p class="empty">Nenhum período de manutenção cadastrado.</p>` : html`
      <table><thead><tr><th>Recurso</th><th>Início</th><th>Fim</th>${isAdmin ? html`<th>Ações</th>` : ''}</tr></thead>
      <tbody>${periods.map((p) => html`<tr><td>${p.resourceId}</td><td>${fmtDate(p.startsAt)}</td><td>${fmtDate(p.endsAt)}</td>
        ${isAdmin ? html`<td><button class="small danger" data-remove="${p.resourceId}">Remover</button></td>` : ''}</tr>`)}</tbody></table>`);
  }

  container.querySelector('#block')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.target));
    if (new Date(data.endsAt) <= new Date(data.startsAt)) { toast('O fim deve ser posterior ao início.', 'error'); return; }
    try {
      await post(`/api/resources/${encodeURIComponent(data.resourceId)}/maintenance`,
        { startsAt: toIso(data.startsAt), endsAt: toIso(data.endsAt) });
      toast('Recurso bloqueado para manutenção.', 'ok'); load();
    } catch (error) { toast(error.message, 'error'); }
  });
  list.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-remove]');
    if (!button || !window.confirm('Remover o período de manutenção?')) return;
    try { await del(`/api/resources/${encodeURIComponent(button.dataset.remove)}/maintenance`); toast('Período removido.', 'ok'); load(); }
    catch (error) { toast(error.message, 'error'); }
  });
  await load();
}
