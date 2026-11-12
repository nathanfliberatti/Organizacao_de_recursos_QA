// Utilitarios de renderizacao. Todo valor interpolado em `html` e escapado, exceto o marcado com raw().
const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
export const esc = (value) => String(value).replace(/[&<>"']/g, (c) => ESCAPES[c]);
export const raw = (markup) => ({ raw: markup });

function render(value) {
  if (value === null || value === undefined || value === false) return '';
  if (Array.isArray(value)) return value.map(render).join('');
  if (typeof value === 'object' && 'raw' in value) return value.raw;
  return esc(value);
}

export function html(strings, ...values) {
  return raw(strings.reduce((out, chunk, i) => out + chunk + (i < values.length ? render(values[i]) : ''), ''));
}
export const toMarkup = (value) => render(value);

export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—');
export const toIso = (localValue) => (localValue ? new Date(localValue).toISOString() : null);
export function toLocalInput(iso) {
  const d = new Date(iso);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}
const STATUS_LABEL = { EM_USO: 'EM USO', NAO_COMPARECEU: 'NÃO COMPARECEU' };
export const badge = (status) => html`<span class="badge ${status}">${STATUS_LABEL[status] || status}</span>`;
export const TYPE_LABEL = { SALA: 'Sala', PROFESSOR: 'Professor', MATERIAL: 'Material' };

export function toast(message, kind = '') {
  const node = document.getElementById('toast');
  node.textContent = message;
  node.className = `toast show ${kind}`;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { node.className = 'toast'; }, 5000);
}

export function openDialog(markup) {
  const dialog = document.getElementById('dialog');
  dialog.innerHTML = toMarkup(markup);
  dialog.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', () => dialog.close()));
  if (!dialog.open) dialog.showModal();
  return dialog;
}
export const closeDialog = () => document.getElementById('dialog').close();
