import { get, post, put, del, api } from '../api.js';
import { html, toMarkup, toIso, toLocalInput, fmtDate, badge, openDialog, closeDialog, toast } from '../util.js';

export async function render(container, { user }) {
  const canSeeAll = user.role !== 'SOLICITANTE';
  const [resources] = await Promise.all([get('/api/resources')]);
  const bookable = resources.filter((r) => r.type !== 'PROFESSOR');
  const professors = resources.filter((r) => r.type === 'PROFESSOR');
  const prefill = sessionStorage.getItem('recursos.prefill');
  sessionStorage.removeItem('recursos.prefill');
  const byId = Object.fromEntries(resources.map((r) => [r.id, r]));

  container.innerHTML = toMarkup(html`
    <h1>Reservas</h1>
    <section class="card" aria-labelledby="new-title">
      <h2 id="new-title">Nova reserva</h2>
      <form id="reserve" class="grid">
        <label>Recurso <select name="resourceId" required>
          <option value="">Selecione…</option>
          ${bookable.map((r) => html`<option value="${r.id}" ${r.id === prefill ? 'selected' : ''}>${r.name} (${r.id})${r.restricted ? ' — restrito' : ''}</option>`)}
        </select></label>
        <label>Professor <select name="professorId">
          <option value="">Eu mesmo (${user.username})</option>
          ${professors.map((p) => html`<option value="${p.id}">${p.name}</option>`)}
        </select></label>
        <label>Início <input name="startsAt" type="datetime-local" required></label>
        <label>Término <input name="endsAt" type="datetime-local" required></label>
        <div class="actions">
          <button type="submit">Reservar</button>
          <button type="button" class="secondary" id="check">Verificar disponibilidade</button>
        </div>
      </form>
      <p class="hint">Recursos restritos ficam como SOLICITADA até a aprovação de um Responsável.</p>
    </section>
    <section class="card" aria-labelledby="list-title">
      <h2 id="list-title">${canSeeAll ? 'Todas as reservas' : 'Minhas reservas'}</h2>
      ${canSeeAll ? html`<label class="inline"><input type="checkbox" id="mine"> Mostrar somente as minhas</label>` : ''}
      <div id="list" class="table-wrap"></div>
    </section>`);

  const list = container.querySelector('#list');
  let rows = [];

  function readPeriod(form) {
    const data = Object.fromEntries(new FormData(form));
    return { ...data, startsAt: toIso(data.startsAt), endsAt: toIso(data.endsAt) };
  }

  const FINAL_STATES = ['CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'];

  function statusOf(r) {
    return r.lifecycleStatus || (r.schedulingStatus === 'CANCELADA' ? 'CANCELADA' : 'SOLICITADA');
  }

  function actionsFor(r) {
    const status = statusOf(r);
    const owner = r.requesterId === user.username;
    const manager = user.role !== 'SOLICITANTE';
    const started = status === 'EM_USO' || status === 'CONCLUIDA';
    const buttons = [];
    if (!FINAL_STATES.includes(status)) {
      if (status === 'SOLICITADA' && user.role === 'RESPONSAVEL' && !owner) {
        buttons.push(['approve', 'Aprovar', ''], ['reject', 'Rejeitar', 'danger']);
      }
      if (status === 'APROVADA' && (owner || manager)) buttons.push(['start', 'Iniciar uso', '']);
      if (status === 'APROVADA' && manager) buttons.push(['noshow', 'Não compareceu', 'secondary']);
      if (status === 'EM_USO' && (owner || manager)) buttons.push(['complete', 'Concluir', '']);
      if (!started && (owner || manager)) {
        buttons.push(['edit', 'Alterar período', 'secondary'], ['cancel', 'Cancelar', 'danger']);
      }
    }
    if (manager) buttons.push(['history', 'Histórico', 'secondary']);
    return buttons.map(([action, label, style]) =>
      html`<button class="small ${style}" data-action="${action}" data-id="${r.id}">${label}</button>`);
  }

  async function load() {
    const mine = container.querySelector('#mine')?.checked;
    try { rows = await get('/api/reservations', { mine }); } catch (error) { toast(error.message, 'error'); return; }
    list.innerHTML = toMarkup(rows.length === 0 ? html`<p class="empty">Nenhuma reserva encontrada.</p>` : html`
      <table>
        <thead><tr><th>Nº</th><th>Recurso</th><th>Professor</th><th>Solicitante</th><th>Início</th><th>Término</th><th>Situação</th><th>Ações</th></tr></thead>
        <tbody>${rows.map((r) => html`<tr>
          <td>${r.id}</td><td>${byId[r.resourceId]?.name || r.resourceId}</td><td>${r.professorId}</td><td>${r.requesterId}</td>
          <td>${fmtDate(r.startsAt)}</td><td>${fmtDate(r.endsAt)}</td>
          <td>${badge(statusOf(r))}</td>
          <td class="actions">${actionsFor(r)}</td></tr>`)}</tbody>
      </table>`);
  }

  container.querySelector('#reserve').addEventListener('submit', async (event) => {
    event.preventDefault();
    const body = readPeriod(event.target);
    if (!body.professorId) delete body.professorId;
    if (new Date(body.endsAt) <= new Date(body.startsAt)) { toast('O término deve ser posterior ao início.', 'error'); return; }
    try {
      const { status, data } = await post('/api/reservations', body, { allow: [409] });
      if (status === 409) toast('Horário indisponível: conflito de agenda ou recurso em manutenção.', 'error');
      else { toast(`Reserva ${data.reservationId} criada.`, 'ok'); load(); }
    } catch (error) { toast(error.message, 'error'); }
  });

  container.querySelector('#check').addEventListener('click', async () => {
    const form = container.querySelector('#reserve');
    const body = readPeriod(form);
    if (!body.resourceId || !body.startsAt || !body.endsAt) { toast('Informe recurso, início e término.', 'error'); return; }
    try {
      const result = await get(`/api/resources/${encodeURIComponent(body.resourceId)}/availability`,
        { professorId: body.professorId || user.username, startsAt: body.startsAt, endsAt: body.endsAt });
      toast(result.available ? 'Horário disponível.' : 'Horário indisponível.', result.available ? 'ok' : 'error');
    } catch (error) { toast(error.message, 'error'); }
  });

  container.querySelector('#mine')?.addEventListener('change', load);
  list.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const { action, id } = button.dataset;
    const reservation = rows.find((r) => r.id === id);
    try {
      if (action === 'cancel') {
        if (!window.confirm(`Cancelar a reserva ${id}?`)) return;
        await del(`/api/reservations/${id}`); toast('Reserva cancelada.', 'ok'); load();
      } else if (['approve', 'reject', 'noshow', 'start', 'complete'].includes(action)) {
        if (action === 'noshow' && !window.confirm(`Registrar que o solicitante não compareceu (reserva ${id})? O horário será liberado.`)) return;
        if (action === 'reject' && !window.confirm(`Rejeitar a solicitação ${id}? O horário será liberado.`)) return;
        const path = { approve: 'approval', reject: 'rejection', noshow: 'no-show', start: 'start', complete: 'complete' }[action];
        const { data } = await post(`/api/reservations/${id}/${path}`);
        const expected = { approve: 'APROVADA', reject: 'REJEITADA', noshow: 'NAO_COMPARECEU', start: 'EM_USO', complete: 'CONCLUIDA' }[action];
        if (data.status === expected) toast('Estado atualizado.', 'ok');
        else toast('A transição não foi aplicada (regra de negócio).', 'error');
        load();
      } else if (action === 'edit') editPeriod(reservation, load);
      else if (action === 'history') showHistory(id);
    } catch (error) {
      toast(error.status === 409 ? `Não foi possível: ${error.message}` : error.message, 'error');
    }
  });
  await load();
}

function editPeriod(reservation, reload) {
  const dialog = openDialog(html`
    <h2>Alterar reserva ${reservation.id}</h2>
    <form id="edit-form" class="grid" style="grid-template-columns:1fr">
      <label>Início <input name="startsAt" type="datetime-local" required value="${toLocalInput(reservation.startsAt)}"></label>
      <label>Término <input name="endsAt" type="datetime-local" required value="${toLocalInput(reservation.endsAt)}"></label>
      <div class="actions"><button type="submit">Salvar</button><button type="button" class="secondary" data-close>Fechar</button></div>
    </form>`);
  dialog.querySelector('#edit-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.target));
    try {
      await put(`/api/reservations/${reservation.id}`, { startsAt: toIso(data.startsAt), endsAt: toIso(data.endsAt) });
      closeDialog(); toast('Reserva alterada.', 'ok'); reload();
    } catch (error) { toast(error.status === 409 ? 'O novo período conflita com outra reserva.' : error.message, 'error'); }
  });
}

async function showHistory(id) {
  try {
    const history = await get(`/api/reservations/${id}/history`);
    openDialog(html`
      <h2>Histórico da reserva ${id}</h2>
      ${history.length === 0 ? html`<p class="empty">Sem registros.</p>` : html`<ol class="timeline">${history.map((h) => html`
        <li><strong>${h.from ? `${h.from} → ` : ''}${h.to}</strong> · ${h.actorId} · ${fmtDate(h.changedAt)}<br><span class="hint">${h.reason}</span></li>`)}</ol>`}
      <button type="button" class="secondary" data-close>Fechar</button>`);
  } catch (error) { toast(error.message, 'error'); }
}
