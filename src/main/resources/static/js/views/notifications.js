import { get } from '../api.js';
import { html, toMarkup, fmtDate } from '../util.js';

const EVENT_LABEL = { CRIACAO: 'Criação', ALTERACAO: 'Alteração', CANCELAMENTO: 'Cancelamento' };

export async function render(container, { user }) {
  const privileged = user.role !== 'SOLICITANTE';
  container.innerHTML = toMarkup(html`
    <h1>Notificações</h1>
    <section class="card">
      <p class="hint">Entrega simulada: cada evento de criação, alteração e cancelamento de reserva fica registrado aqui.</p>
      ${privileged ? html`<label class="inline"><input type="checkbox" id="all"> Mostrar de todos os usuários</label>` : ''}
      <div id="list" class="table-wrap"></div>
    </section>`);
  const list = container.querySelector('#list');
  async function load() {
    const items = await get('/api/notifications', { all: container.querySelector('#all')?.checked || undefined });
    list.innerHTML = toMarkup(items.length === 0 ? html`<p class="empty">Nenhuma notificação.</p>` : html`
      <table><thead><tr><th>Quando</th><th>Evento</th><th>Reserva</th><th>Destinatário</th><th>Mensagem</th><th>Entrega</th></tr></thead>
      <tbody>${items.map((n) => html`<tr><td>${fmtDate(n.createdAt)}</td><td>${EVENT_LABEL[n.event]}</td><td>${n.reservationId}</td>
        <td>${n.recipientId}</td><td>${n.message}</td><td>${n.delivery}</td></tr>`)}</tbody></table>`);
  }
  container.querySelector('#all')?.addEventListener('change', load);
  await load();
}
