import { test, expect } from '@playwright/test';

test.describe('MOD-10: 服务健康检查', () => {

  test('后端 Actuator 健康端点', async ({ request }) => {
    const res = await request.get('http://localhost:8080/actuator/health');
    expect(res.ok()).toBe(true);
  });

  test('前端页面可访问', async ({ request }) => {
    const res = await request.get('http://localhost:5174/');
    expect(res.ok()).toBe(true);
  });
});
