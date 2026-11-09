import { get, post } from '../api.js';
import { html, toMarkup, fmtDate, toast } from '../util.js';

export async function render(container) {
  const materials = await get('/api/resources', { type: 'MATERIAL' });
  container.innerHTML = toMarkup(html`
    <h1>Retirada e devolução de materiais</h1>
    <section class="card" aria-labelledby="w-title">
      <h2 id="w-title">Registrar retirada</h2>
      <form id="withdraw" class="grid">
        <label>Material <select name="resourceId" required><option value="">Selecione…</option>
          ${materials.map((m) => html`<option value="${m.id}">${m.name} (${m.id})</option>`)}</select></label>
        <label>Retirado por <input name="borrowerId" required placeholder="usuário ou nome"></label>
        <label>Observações <input name="notes"></label>
        <button type="submit">Registrar retirada</button>
      </form>
      ${materials.length === 0 ? html`<p class="hint">Não há materiais cadastrados. Cadastre um recurso do tipo Material em Recursos.</p>` : ''}
    </section>
    <section class="card"><h2>Movimentações</h2>
      <label class="inline"><input type="checkbox" id="only-open"> Somente em aberto</label>
      <div id="list" class="table-wrap"></div></section>`);
  const list = container.querySelector('#list');

  async function load() {
    const loans = await get('/api/loans', { open: container.querySelector('#only-open').checked ? true : undefined });
    list.innerHTML = toMarkup(loans.length === 0 ? html`<p class="empty">Nenhuma movimentação registrada.</p>` : html`
      <table><thead><tr><th>Nº</th><th>Material</th><th>Retirado por</th><th>Registrado por</th><th>Retirada</th><th>Devolução</th><th>Situação</th><th>Ações</th></tr></thead>
      <tbody>${loans.map((l) => html`<tr><td>${l.id}</td><td>${l.resourceId}</td><td>${l.borrowerId}</td><td>${l.registeredBy}</td>
        <td>${fmtDate(l.withdrawnAt)}</td><td>${fmtDate(l.returnedAt)}</td>
        <td>${l.returnedAt ? html`<span class="badge CONCLUIDA">DEVOLVIDO</span>` : html`<span class="badge EM_USO">RETIRADO</span>`}</td>
        <td>${l.returnedAt ? '' : html`<button class="small" data-return="${l.id}">Registrar devolução</button>`}</td></tr>`)}</tbody></table>`);
  }

  container.querySelector('#withdraw').addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.target));
    const { resourceId, ...body } = data;
    try {
      await post(`/api/materials/${encodeURIComponent(resourceId)}/withdrawals`, body);
      toast('Retirada registrada.', 'ok'); event.target.reset(); load();
    } catch (error) { toast(error.status === 409 ? 'Este material já está retirado.' : error.message, 'error'); }
  });
  container.querySelector('#only-open').addEventListener('change', load);
  list.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-return]');
    if (!button) return;
    try { await post(`/api/loans/${button.dataset.return}/return`); toast('Devolução registrada.', 'ok'); load(); }
    catch (error) { toast(error.message, 'error'); }
  });
  await load();
}
