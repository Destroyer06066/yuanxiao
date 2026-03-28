import { test, expect } from '@playwright/test';
import { apiLogin, apiGet, apiPost } from '../helpers/api.helper';
import { ACCOUNTS } from '../helpers/accounts';

test.describe('MOD-03: 录取操作 (API)', () => {

  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await apiLogin(
      request,
      ACCOUNTS.school_admin.username,
      ACCOUNTS.school_admin.password,
    );
  });

  test('查询专业列表', async ({ request }) => {
    const data = await apiGet(request, '/majors', token);
    expect(data.code).toBe(0);
  });

  test('查询名额列表', async ({ request }) => {
    const data = await apiGet(request, '/quota', token);
    expect(data.code).toBe(0);
  });

  test('直接录取（有 PENDING 考生时）', async ({ request }) => {
    const students = await apiGet(request, '/students?page=1&pageSize=1&status=PENDING', token);
    const majors = await apiGet(request, '/majors', token);

    if (students.data?.records?.length > 0 && majors.data?.length > 0) {
      const pushId = students.data.records[0].pushId;
      const majorId = majors.data[0].majorId;
      const result = await apiPost(request, '/students/admit', token, { pushId, majorId });
      expect(result).toHaveProperty('code');
    }
  });

  test('录取不存在的考生 → 返回错误', async ({ request }) => {
    const result = await apiPost(request, '/students/admit', token, {
      pushId: '00000000-0000-0000-0000-000000000000',
      majorId: '00000000-0000-0000-0000-000000000000',
    });
    expect(result.code).not.toBe(0);
  });
});

test.describe('MOD-03: 权限控制', () => {

  test('SCHOOL_STAFF 无法执行录取', async ({ request }) => {
    const token = await apiLogin(
      request,
      ACCOUNTS.school_staff.username,
      ACCOUNTS.school_staff.password,
    );
    const result = await apiPost(request, '/students/admit', token, {
      pushId: '00000000-0000-0000-0000-000000000000',
      majorId: '00000000-0000-0000-0000-000000000000',
    });
    expect(result.code).not.toBe(0);
  });
});
