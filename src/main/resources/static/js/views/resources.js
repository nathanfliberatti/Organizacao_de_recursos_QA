import { get, post, put } from '../api.js';
import { html, toMarkup, toIso, openDialog, closeDialog, toast, TYPE_LABEL } from '../util.js';

const TYPES = Object.entries(TYPE_LABEL);

export async function render(container, { user, navigate }) {
  const isAdmin = user.role === 'ADMINISTRADOR';
  container.innerHTML = toMarkup(html`
    <h1>Recursos</h1>
    <section class="card" aria-label="Pesquisa de recursos">
      <form id="search" class="grid">
        <label>Tipo <select name="type"><option value="">Todos</option>${TYPES.map(([v, l]) => html`<option value="${v}">${l}</option>`)}</select></label>
        <label>Capacidade mínima <input name="minCapacity" type="number" min="1"></label>
        <label>Localização <input name="location"></label>
        <label>Competência <input name="competency"></label>
        <label>Texto (id ou nome) <input name="text"></label>
        <label>Disponível a partir de <input name="availableFrom" type="datetime-local"></label>
        <label>Disponível até <input name="availableTo" type="datetime-local"></label>
        <div class="actions"><button type="submit">Pesquisar</button><button type="reset" class="secondary">Limpar</button></div>
      </form>
    </section>
    ${isAdmin ? html`<p><button id="new-resource">Novo recurso</button></p>` : ''}
    <div class="card"><div id="results" class="table-wrap"></div></div>`);

  const results = container.querySelector('#results');
  let current = [];

  async function load() {
    const form = Object.fromEntries(new FormData(container.querySelector('#search')));
    const query = { ...form, availableFrom: toIso(form.availableFrom), availableTo: toIso(form.availableTo) };
    if (!!query.availableFrom !== !!query.availableTo) { toast('Informe o início e o fim da disponibilidade.', 'error'); return; }
    try {
      current = await get('/api/resources', query);
    } catch (error) { toast(error.message, 'error'); return; }
    results.innerHTML = toMarkup(current.length === 0 ? html`<p class="empty">Nenhum recurso encontrado.</p>` : html`
      <table>
        <thead><tr><th>Id</th><th>Nome</th><th>Tipo</th><th>Capacidade</th><th>Local</th><th>Competência</th><th>Restrito</th><th>Ações</th></tr></thead>
        <tbody>${current.map((r) => html`<tr>
          <td>${r.id}</td><td>${r.name}</td><td>${TYPE_LABEL[r.type]}</td><td>${r.capacity}</td>
          <td>${r.location || '—'}</td><td>${r.competency || '—'}</td><td>${r.restricted ? 'Sim' : 'Não'}</td>
          <td class="actions">
            ${r.type !== 'PROFESSOR' ? html`<button class="small" data-reserve="${r.id}">Reservar</button>` : ''}
            ${isAdmin ? html`<button class="small secondary" data-edit="${r.id}">Editar</button>` : ''}
          </td></tr>`)}</tbody>
      </table>`);
  }

  container.querySelector('#search').addEventListener('submit', (e) => { e.preventDefault(); load(); });
  container.querySelector('#search').addEventListener('reset', () => setTimeout(load));
  results.addEventListener('click', (event) => {
    const reserve = event.target.closest('[data-reserve]');
    if (reserve) { sessionStorage.setItem('recursos.prefill', reserve.dataset.reserve); navigate('reservas'); }
    const edit = event.target.closest('[data-edit]');
    if (edit) editForm(current.find((r) => r.id === edit.dataset.edit), load);
  });
  container.querySelector('#new-resource')?.addEventListener('click', () => editForm(null, load));
  await load();
}

function editForm(resource, reload) {
  const editing = !!resource;
  const r = resource || { type: 'SALA', capacity: 1, restricted: false };
  const dialog = openDialog(html`
    <h2>${editing ? 'Editar recurso' : 'Novo recurso'}</h2>
    <form id="resource-form" class="grid" style="grid-template-columns:1fr">
      <label>Id <input name="id" value="${r.id || ''}" ${editing ? 'readonly' : 'required'}></label>
      <label>Nome <input name="name" value="${r.name || ''}"></label>
      <label>Tipo <select name="type">${TYPES.map(([v, l]) => html`<option value="${v}" ${v === r.type ? 'selected' : ''}>${l}</option>`)}</select></label>
      <label>Capacidade <input name="capacity" type="number" min="1" required value="${r.capacity}"></label>
      <label>Localização <input name="location" value="${r.location || ''}"></label>
      <label>Competência <input name="competency" value="${r.competency || ''}"></label>
      <label class="inline"><input name="restricted" type="checkbox" ${r.restricted ? 'checked' : ''}> Recurso restrito (exige aprovação do Responsável)</label>
      <div class="actions"><button type="submit">Salvar</button><button type="button" class="secondary" data-close>Cancelar</button></div>
    </form>`);
  dialog.querySelector('#resource-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const body = {
      id: form.get('id').trim(), name: form.get('name').trim() || null, type: form.get('type'),
      capacity: Number(form.get('capacity')), location: form.get('location'), competency: form.get('competency'),
      restricted: form.get('restricted') === 'on',
    };
    try {
      if (editing) await put(`/api/resources/${encodeURIComponent(r.id)}`, body); else await post('/api/resources', body);
      closeDialog(); toast('Recurso salvo.', 'ok'); reload();
    } catch (error) { toast(error.status === 409 ? 'Já existe um recurso com este id.' : error.message, 'error'); }
  });
}
