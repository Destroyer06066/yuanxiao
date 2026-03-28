import { test as setup } from '@playwright/test';
import { ACCOUNTS, API_BASE } from './helpers/accounts';

const authenticate = async (
  projectName: string,
  page: import('@playwright/test').Page,
) => {
  const account = ACCOUNTS[projectName];
  if (!account) return;

  const res = await page.request.post(`${API_BASE}/auth/login`, {
    data: { username: account.username, password: account.password },
  });
  const body = await res.json();
  if (body.code !== 0) throw new Error(`Setup login failed for ${projectName}: ${body.message}`);
  const token = body.data.accessToken;

  await page.goto('http://localhost:5174/login');
  await page.evaluate((t) => {
    localStorage.setItem('access_token', t);
  }, token);

  await page.goto('http://localhost:5174/dashboard');
  await page.waitForLoadState('networkidle');

  await page.context().storageState({ path: `.auth/${projectName}.json` });
};

setup('authenticate as op_admin', async ({ page }) => {
  await authenticate('op_admin', page);
});

setup('authenticate as school_admin', async ({ page }) => {
  await authenticate('school_admin', page);
});

setup('authenticate as school_staff', async ({ page }) => {
  await authenticate('school_staff', page);
});
