import { test, expect } from '@playwright/test';
import { StatisticsPage } from '../pages/statistics.page';
import { apiLogin, apiGet } from '../helpers/api.helper';
import { ACCOUNTS } from '../helpers/accounts';

test.describe('MOD-08: 数据统计 (API)', () => {

  test('KPI 统计接口', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.op_admin.username,
      ACCOUNTS.op_admin.password,
    );
    const data = await apiGet(request, '/statistics/kpis', token);
    expect(data.code).toBe(0);
    expect(data.data).toBeDefined();
  });

  test('院校进度接口', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.op_admin.username,
      ACCOUNTS.op_admin.password,
    );
    const data = await apiGet(request, '/statistics/school-progress', token);
    expect(data.code).toBe(0);
  });
});

test.describe('MOD-08: 数据统计 (UI)', () => {
  test.use({ storageState: '.auth/school_admin.json' });

  test('统计页面加载 → KPI 卡片可见', async ({ page }) => {
    const statsPage = new StatisticsPage(page);
    await statsPage.goto();
    await expect(page.locator('.el-card').first()).toBeVisible();
  });
});
