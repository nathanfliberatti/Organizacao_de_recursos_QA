import { api } from '../api.js';
import { html, toMarkup, toast } from '../util.js';

export async function render(container) {
  const now = new Date();
  const month = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, '0')}`;
  container.innerHTML = toMarkup(html`
    <h1>Relatório mensal de utilização</h1>
    <section class="card">
      <form id="report-form" class="grid">
        <label>Mês <input type="month" name="month" value="${month}" required></label>
        <button type="submit">Gerar relatório</button>
      </form>
      <p class="hint">O mês é fechado em UTC (do dia 1 às 00:00Z até o dia 1 do mês seguinte, exclusivo).</p>
    </section>
    <div id="out" aria-live="polite"></div>`);
  const out = container.querySelector('#out');

  async function generate(value) {
    const [year, mon] = value.split('-').map(Number);
    const inicio = new Date(Date.UTC(year, mon - 1, 1)).toISOString();
    const fim = new Date(Date.UTC(year, mon, 1)).toISOString();
    try {
      const { data } = await api('/relatorios/utilizacao', { query: { inicio, fim } });
      out.innerHTML = toMarkup(html`
        <div class="card"><h2>Conflitos evitados</h2><p class="kpi">${data.conflitosEvitados}</p></div>
        <div class="card"><h2>Horas reservadas por recurso</h2>${table(['Recurso', 'Horas'], data.horasPorRecurso.map((x) => [x.recursoId, x.horasReservadas.toFixed(2)]))}</div>
        <div class="card"><h2>Carga horária por professor</h2>${table(['Professor', 'Horas'], data.cargaPorProfessor.map((x) => [x.professorId, x.horasAlocadas.toFixed(2)]))}</div>`);
    } catch (error) {
      out.innerHTML = '';
      toast(error.status === 404 ? 'O relatório exige a aplicação executando com o perfil postgres.' : error.message, 'error');
    }
  }
  container.querySelector('#report-form').addEventListener('submit', (e) => { e.preventDefault(); generate(new FormData(e.target).get('month')); });
}

function table(headers, rows) {
  if (rows.length === 0) return html`<p class="empty">Sem dados no período.</p>`;
  return html`<div class="table-wrap"><table><thead><tr>${headers.map((h) => html`<th>${h}</th>`)}</tr></thead>
    <tbody>${rows.map((r) => html`<tr>${r.map((c) => html`<td>${c}</td>`)}</tr>`)}</tbody></table></div>`;
}
