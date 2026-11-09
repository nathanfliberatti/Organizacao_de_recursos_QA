const TOKEN_KEY = 'recursos.token';
const USER_KEY = 'recursos.user';

export const session = {
  get token() { return sessionStorage.getItem(TOKEN_KEY); },
  get user() { const raw = sessionStorage.getItem(USER_KEY); return raw ? JSON.parse(raw) : null; },
  save(token, user) { sessionStorage.setItem(TOKEN_KEY, token); sessionStorage.setItem(USER_KEY, JSON.stringify(user)); },
  clear() { sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(USER_KEY); },
};

export class ApiError extends Error {
  constructor(status, message, body) { super(message); this.status = status; this.body = body; }
}

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

const FRIENDLY = {
  400: 'Dados inválidos.', 401: 'Sessão expirada ou credenciais inválidas.',
  403: 'Você não tem permissão para esta ação.', 404: 'Item não encontrado.', 409: 'Conflito com o estado atual.',
};

export async function api(path, { method = 'GET', body, query, allow } = {}) {
  const url = new URL(path, window.location.origin);
  Object.entries(query || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v); });
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (session.token) headers.Authorization = `Bearer ${session.token}`;
  const response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (response.ok || (allow && allow.includes(response.status))) return { status: response.status, data };
  if (response.status === 401 && session.token && path !== '/api/auth/login') { onUnauthorized(); }
  throw new ApiError(response.status, (data && (data.detail || data.message)) || FRIENDLY[response.status] || 'Erro inesperado.', data);
}

export const get = (path, query) => api(path, { query }).then((r) => r.data);
export const post = (path, body, opts = {}) => api(path, { method: 'POST', body: body ?? {}, ...opts });
export const put = (path, body) => api(path, { method: 'PUT', body }).then((r) => r.data);
export const del = (path) => api(path, { method: 'DELETE' });
