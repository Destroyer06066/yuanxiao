import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { Sidebar } from '../pages/sidebar.component';
import { API_BASE, ACCOUNTS } from '../helpers/accounts';

test.describe('MOD-01: 登录与权限', () => {

  test('正确凭据登录成功 → 跳转首页', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(ACCOUNTS.op_admin.username, ACCOUNTS.op_admin.password);
    await expect(page).toHaveURL(/dashboard/);
  });

  test('错误密码登录失败 → 显示错误提示', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(ACCOUNTS.op_admin.username, 'wrong_password');
    await expect(loginPage.errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('未登录访问受保护页面 → 跳转登录页', async ({ browser }) => {
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto('http://localhost:5174/dashboard');
    await expect(page).toHaveURL(/login/);
    await context.close();
  });
});

test.describe('MOD-01: 角色侧边栏', () => {
  test.use({ storageState: '.auth/op_admin.json' });

  test('OP_ADMIN 可见院校管理菜单', async ({ page }) => {
    await page.goto('/dashboard');
    const sidebar = new Sidebar(page);
    await expect(sidebar.menuItem('院校管理')).toBeVisible();
  });
});

test.describe('MOD-01: SCHOOL_ADMIN 侧边栏', () => {
  test.use({ storageState: '.auth/school_admin.json' });

  test('SCHOOL_ADMIN 可见考生列表菜单', async ({ page }) => {
    await page.goto('/dashboard');
    const sidebar = new Sidebar(page);
    await expect(sidebar.menuItem('考生')).toBeVisible();
  });
});

test.describe('MOD-13: 安全测试', () => {

  test('无效 Token 请求 API → 401', async ({ request }) => {
    const res = await request.get(`${API_BASE}/students?page=1&pageSize=20`, {
      headers: { Authorization: 'Bearer invalid_token' },
    });
    expect([401, 403]).toContain(res.status());
  });

  test('无 Token 请求 API → 401', async ({ request }) => {
    const res = await request.get(`${API_BASE}/students?page=1&pageSize=20`);
    expect([401, 403]).toContain(res.status());
  });
});
