import { APIRequestContext } from '@playwright/test';
import { API_BASE } from './accounts';

export async function apiLogin(
  request: APIRequestContext,
  username: string,
  password: string,
): Promise<string> {
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: { username, password },
  });
  const body = await res.json();
  if (body.code !== 0) throw new Error(`Login failed: ${body.message}`);
  return body.data.accessToken;
}

export async function apiGet(
  request: APIRequestContext,
  path: string,
  token: string,
) {
  const res = await request.get(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json();
}

export async function apiPost(
  request: APIRequestContext,
  path: string,
  token: string,
  data?: unknown,
) {
  const res = await request.post(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  });
  return res.json();
}
