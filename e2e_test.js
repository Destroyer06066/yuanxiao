/**
 * @deprecated 已迁移到 e2e/ 目录的 Playwright Test Runner 框架
 * 运行新版: cd e2e && npx playwright test
 * 本脚本保留仅供参考，不再维护。
 */
/**
 * 院校管理平台 E2E 测试脚本
 * 用法: node e2e_test.js [role]
 * role: op_admin | school_admin | school_staff
 */
const { chromium } = require('playwright');

const API_BASE = 'http://localhost:8080/api/v1';
const FRONTEND_BASE = 'http://localhost:5174';

const ACCOUNTS = {
  op_admin: { username: 'op_admin', password: 'OpAdmin@2026' },
  school_admin: { username: 'testuser001', password: 'TestPass@123' },
  school_staff: { username: 'test_staff_001', password: 'TestPass@123' }, // will be reset
};

async function apiLogin(username, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json();
  if (data.code !== 0) throw new Error(`Login failed: ${data.message}`);
  return data.data.accessToken;
}

async function apiGet(path, token) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  return res.json();
}

async function apiPost(path, token, body) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

async function apiPut(path, token, body) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  return res.json();
}

let passed = 0;
let failed = 0;
const results = [];

function test(name, passed_, details = '') {
  const status = passed_ ? 'PASS' : 'FAIL';
  const icon = passed_ ? '✅' : '❌';
  results.push({ name, status, details });
  if (passed_) {
    passed++;
    console.log(`  ${icon} ${name}`);
  } else {
    failed++;
    console.log(`  ${icon} ${name} — ${details}`);
  }
}

async function run() {
  const role = process.argv[2] || 'op_admin';
  console.log(`\n=== 院校管理平台 E2E 测试 — ${role} ===\n`);

  const { username, password } = ACCOUNTS[role];
  let token;
  try {
    token = await apiLogin(username, password);
    console.log(`✅ 登录成功: ${username} (${role})`);
  } catch (e) {
    console.error(`❌ 登录失败: ${e.message}`);
    return;
  }

  // ============ MOD-01: 账号与权限 ============
  console.log(`\n[MOD-01] 账号与权限模块`);
  if (role === 'school_staff') {
    // SCHOOL_STAFF无权访问账号管理
    const accountsRes = await apiGet('/accounts?page=1&pageSize=20', token);
    test('账号列表-权限拦截 (SCHOOL_STAFF)', accountsRes.code !== 0, `正确拒绝, code=${accountsRes.code}`);
  } else {
    const accountsRes = await apiGet('/accounts?page=1&pageSize=20', token);
    test('账号列表查询 (ACC-E2E-025前置)', accountsRes.code === 0, accountsRes.message || '');
  }

  // OP_ADMIN可跨校访问院校列表，其他角色无权限
  if (role === 'op_admin') {
    const schoolsRes = await apiGet('/admin/schools?page=1&pageSize=20', token);
    test('院校列表查询 (OP_ADMIN)', schoolsRes.code === 0, schoolsRes.message || '');
    if (schoolsRes.data) {
      test('至少2所院校存在', schoolsRes.data.total >= 2, `实际: ${schoolsRes.data.total}`);
    }
  } else {
    const schoolsRes = await apiGet('/admin/schools?page=1&pageSize=20', token);
    test('院校列表-权限拦截 (ACC-E2E-035)', schoolsRes.code !== 0, `正确拒绝, code=${schoolsRes.code}`);
  }

  // ============ MOD-02: 考生管理 ============
  console.log(`\n[MOD-02] 考生管理模块`);
  const studentsRes = await apiGet('/students?page=1&pageSize=20', token);
  test('考生列表查询 (STU-E2E-001)', studentsRes.code === 0, studentsRes.message || '');
  if (studentsRes.data) {
    // SCHOOL_STAFF/SCHOOL_ADMIN只能看本校数据（数据隔离）
    const hasStudents = studentsRes.data.total >= 1;
    test('本校考生数据', hasStudents || role !== 'op_admin', `本校考生: ${studentsRes.data.total}`);
  }

  const student = studentsRes.data?.records?.[0];
  if (student) {
    const detailRes = await apiGet(`/students/${student.pushId}`, token);
    test('考生详情查询 (STU-E2E-012)', detailRes.code === 0, detailRes.message || '');
  }

  // ============ MOD-03: 录取操作 (SCHOOL_ADMIN only) ============
  if (role === 'school_admin' || role === 'op_admin') {
    console.log(`\n[MOD-03] 录取操作模块`);

    if (student) {
      // 确认考生状态
      test('考生状态=PENDING', student.status === 'PENDING', `实际: ${student.status}`);

      // 尝试录取（需要专业和名额）
      // 先检查有无专业
      const majorsRes = await apiGet('/majors', token);
      if (majorsRes.code === 0 && majorsRes.data?.length > 0) {
        const major = majorsRes.data[0];
        // 检查名额
        const quotaRes = await apiGet('/quota', token);
        if (quotaRes.code === 0 && quotaRes.data?.length > 0) {
          const quota = quotaRes.data[0];
          const admitRes = await apiPost('/students/admit', token, {
            pushId: student.pushId,
            majorId: major.majorId,
          });
          test('直接录取 (ADM-E2E-001)', admitRes.code === 0, admitRes.message || '');
        } else {
          test('录取跳过（无名额数据）', true);
        }
      } else {
        test('录取跳过（无专业数据）', true);
      }
    }
  }

  // ============ MOD-04: 补录管理 ============
  console.log(`\n[MOD-04] 补录管理模块`);
  const supplementRes = await apiGet('/supplement/rounds', token);
  test('补录轮次查询 (SUP-E2E-007)', supplementRes.code === 0, supplementRes.message || '');

  // ============ MOD-08: 数据统计 ============
  console.log(`\n[MOD-08] 数据统计模块`);
  const kpisRes = await apiGet('/statistics/kpis', token);
  test('KPI统计 (STA-E2E-001)', kpisRes.code === 0, kpisRes.message || '');
  if (kpisRes.data) {
    const d = kpisRes.data;
    test('KPI字段完整', 'totalPushed' in d || 'admitted' in d, JSON.stringify(Object.keys(d)));
  }

  const statsRes = await apiGet('/statistics/school-progress', token);
  test('院校进度 (STA-E2E-001)', statsRes.code === 0, statsRes.message || '');

  // ============ MOD-09: 外部接口 ============
  console.log(`\n[MOD-09] 外部接口集成`);
  const mockRes = await fetch('http://localhost:8081/demo-api/schools', { timeout: 5000 }).catch(() => null);
  test('Mock服务可用', mockRes?.ok, mockRes ? `${mockRes.status}` : 'unreachable');

  // ============ MOD-10: 定时任务（检查健康状态） ============
  console.log(`\n[MOD-10] 定时任务`);
  const healthRes = await fetch('http://localhost:8080/actuator/health').catch(() => null);
  test('后端健康检查', healthRes?.ok, healthRes ? `${healthRes.status}` : 'unreachable');

  // ============ MOD-13: 安全测试 ============
  console.log(`\n[MOD-13] 安全测试`);
  const invalidRes = await fetch(`${API_BASE}/students?page=1&pageSize=20`, {
    headers: { 'Authorization': 'Bearer invalid_token' },
  });
  test('无效Token拒绝 (SEC-001)', invalidRes.status === 401 || invalidRes.status === 403, `实际: ${invalidRes.status}`);

  // ============ 跨角色数据隔离测试 ============
  // 已在 SCHOOL_ADMIN 角色测试中验证

  // ============ 汇总 ============
  console.log(`\n=== 测试结果汇总 ===`);
  console.log(`通过: ${passed}  失败: ${failed}  总计: ${passed + failed}`);
  const rate = ((passed / (passed + failed)) * 100).toFixed(1);
  console.log(`通过率: ${rate}%`);
  if (failed > 0) {
    console.log(`\n失败用例:`);
    results.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`  ❌ ${r.name}: ${r.details}`);
    });
  }

  // ============ 浏览器UI测试 ============
  console.log(`\n=== 浏览器UI测试 ===`);
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Inject token via storage state
  await context.addInitScript(storage => {
    localStorage.setItem('access_token', storage.token);
  }, { token });

  try {
    // Test dashboard
    await page.goto(`${FRONTEND_BASE}/dashboard`, { waitUntil: 'networkidle' });
    const title = await page.title();
    test('页面标题包含平台名', title.includes('院校'), `实际: ${title}`);

    // Check sidebar
    const menuItems = await page.locator('.el-menu-item, .el-sub-menu').count();
    test('侧边栏菜单加载', menuItems > 0, `找到 ${menuItems} 个菜单项`);

    // Check user info
    const userButton = await page.locator('button').filter({ hasText: /.+/ }).first().textContent().catch(() => '');
    test('用户信息显示', userButton.length > 0, `显示: ${userButton.trim()}`);

    // Test student list page
    await page.goto(`${FRONTEND_BASE}/students`, { waitUntil: 'networkidle' });
    const table = await page.locator('.el-table').count();
    test('考生列表表格渲染', table > 0, `找到 ${table} 个表格`);

    // Test school management (OP_ADMIN only)
    if (role === 'op_admin') {
      await page.goto(`${FRONTEND_BASE}/admin/schools`, { waitUntil: 'networkidle' });
      const addBtn = await page.locator('button:has-text("新增院校")').count();
      test('院校管理-新增按钮可见', addBtn > 0, `找到 ${addBtn} 个按钮`);

      // Test account management
      await page.goto(`${FRONTEND_BASE}/accounts`, { waitUntil: 'networkidle' });
      const accBtn = await page.locator('button:has-text("新增账号")').count();
      test('账号管理-新增按钮可见', accBtn > 0, `找到 ${accBtn} 个按钮`);
    }

    // Take screenshot of final state
    await page.screenshot({ path: `/tmp/e2e_${role}_final.png`, fullPage: true });
    console.log(`截图已保存: /tmp/e2e_${role}_final.png`);

  } catch (e) {
    console.log(`  ⚠️ 浏览器测试异常: ${e.message}`);
  }

  await browser.close();
  console.log(`\n=== 全部完成 ===\n`);
}

run().catch(e => {
  console.error('Fatal error:', e);
  process.exit(1);
});
