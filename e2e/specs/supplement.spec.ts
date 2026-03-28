import { test, expect } from '@playwright/test';
import { apiLogin, apiGet } from '../helpers/api.helper';
import { ACCOUNTS } from '../helpers/accounts';

test.describe('MOD-04: 补录管理', () => {

  test('查询补录轮次', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.school_admin.username,
      ACCOUNTS.school_admin.password,
    );
    const data = await apiGet(request, '/supplement/rounds', token);
    expect(data.code).toBe(0);
  });

  test.describe('UI', () => {
    test.use({ storageState: '.auth/school_admin.json' });

    test('补录管理页面加载', async ({ page }) => {
      await page.goto('/supplement');
      await page.waitForLoadState('networkidle');
      await expect(page.locator('.el-card, .el-empty')).toBeVisible();
    });
  });
});
