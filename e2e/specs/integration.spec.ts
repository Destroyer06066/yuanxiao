import { test, expect } from '@playwright/test';

test.describe('MOD-09: 外部接口集成', () => {

  test('Mock 服务健康检查', async ({ request }) => {
    const res = await request.get('http://localhost:8081/demo-api/schools', {
      timeout: 5000,
    }).catch(() => null);

    if (res) {
      expect(res.ok()).toBe(true);
    } else {
      test.skip(true, 'Mock 服务未启动');
    }
  });
});
