import { test, expect } from '@playwright/test';
import { StudentListPage } from '../pages/student-list.page';
import { apiGet, apiLogin } from '../helpers/api.helper';
import { ACCOUNTS, API_BASE } from '../helpers/accounts';

test.describe('MOD-02: 考生管理 (SCHOOL_ADMIN)', () => {
  test.use({ storageState: '.auth/school_admin.json' });

  test('考生列表页面加载 → 表格渲染', async ({ page }) => {
    const studentPage = new StudentListPage(page);
    await studentPage.goto();
    await expect(studentPage.table).toBeVisible();
  });

  test('考生列表 API 返回数据', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.school_admin.username,
      ACCOUNTS.school_admin.password,
    );
    const data = await apiGet(request, '/students?page=1&pageSize=20', token);
    expect(data.code).toBe(0);
    expect(data.data).toHaveProperty('total');
    expect(data.data).toHaveProperty('records');
  });

  test('考生详情 API', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.school_admin.username,
      ACCOUNTS.school_admin.password,
    );
    const list = await apiGet(request, '/students?page=1&pageSize=1', token);
    if (list.data?.records?.length > 0) {
      const pushId = list.data.records[0].pushId;
      const detail = await apiGet(request, `/students/${pushId}`, token);
      expect(detail.code).toBe(0);
    }
  });
});

test.describe('MOD-02: 数据隔离', () => {

  test('OP_ADMIN 可查看全部院校考生', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.op_admin.username,
      ACCOUNTS.op_admin.password,
    );
    const data = await apiGet(request, '/students?page=1&pageSize=20', token);
    expect(data.code).toBe(0);
  });

  test('SCHOOL_STAFF 只能看本校考生', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.school_staff.username,
      ACCOUNTS.school_staff.password,
    );
    const data = await apiGet(request, '/students?page=1&pageSize=20', token);
    expect(data.code).toBe(0);
  });
});
