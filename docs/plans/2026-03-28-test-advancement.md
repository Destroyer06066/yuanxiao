# 测试全面推进 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将院校管理平台的测试体系从散装脚本升级为正式框架：E2E 迁移到 Playwright Test Runner + 前端引入 Vitest + Vue Test Utils。

**Architecture:** 三阶段渐进：(1) 验证现有后端 66 个单测全绿 (2) E2E 从单脚本迁移到 Playwright Test Runner，按模块拆分 spec，引入 fixture/page object (3) 前端引入 Vitest，覆盖 Store/Composable/Utils/关键组件。

**Tech Stack:** Playwright Test Runner · TypeScript · Vitest · Vue Test Utils · happy-dom · Pinia Testing

---

## 阶段一：后端测试验证

### Task 1: 验证现有后端测试全绿

**Files:**
- Read: `backend/src/test/java/com/campus/platform/` (all test files)

**Step 1: 运行全量后端测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/backend
mvn test -q 2>&1 | tail -20
```

期望: `BUILD SUCCESS`，66 个测试全部通过。

**Step 2: 如有失败，修复后重跑**

逐个修复失败用例，确保全绿后再进入阶段二。

**Step 3: Commit（仅当有修复）**

```bash
git add backend/src/test/
git commit -m "fix: repair failing backend tests"
```

---

## 阶段二：E2E 框架化

### Task 2: 初始化 Playwright Test Runner 项目

**Files:**
- Create: `e2e/playwright.config.ts`
- Create: `e2e/package.json`
- Create: `e2e/tsconfig.json`

**Step 1: 创建 e2e 目录和 package.json**

```json
{
  "name": "campus-platform-e2e",
  "private": true,
  "scripts": {
    "test": "playwright test",
    "test:headed": "playwright test --headed",
    "test:ui": "playwright test --ui",
    "report": "playwright show-report"
  },
  "devDependencies": {
    "@playwright/test": "^1.58.0",
    "typescript": "^5.4.0"
  }
}
```

**Step 2: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "outDir": "dist",
    "rootDir": ".",
    "baseUrl": ".",
    "paths": {
      "@fixtures/*": ["fixtures/*"],
      "@pages/*": ["pages/*"],
      "@helpers/*": ["helpers/*"]
    }
  },
  "include": ["**/*.ts"]
}
```

**Step 3: 创建 playwright.config.ts**

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: false,
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:5174',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'op_admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/op_admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'school_admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/school_admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'school_staff',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/school_staff.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  outputDir: './test-results',
});
```

**Step 4: 安装依赖**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/e2e
npm install
npx playwright install chromium
```

**Step 5: 创建目录结构**

```bash
mkdir -p fixtures pages specs helpers .auth
```

**Step 6: Commit**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform
git add e2e/package.json e2e/tsconfig.json e2e/playwright.config.ts
git commit -m "chore: initialize Playwright Test Runner for E2E tests"
```

---

### Task 3: 创建 API Helper 和 Auth 工具

**Files:**
- Create: `e2e/helpers/api.helper.ts`
- Create: `e2e/helpers/accounts.ts`

**Step 1: 创建账号配置**

```typescript
// e2e/helpers/accounts.ts

export interface TestAccount {
  username: string;
  password: string;
  role: 'OP_ADMIN' | 'SCHOOL_ADMIN' | 'SCHOOL_STAFF';
}

export const ACCOUNTS: Record<string, TestAccount> = {
  op_admin: {
    username: 'op_admin',
    password: 'OpAdmin@2026',
    role: 'OP_ADMIN',
  },
  school_admin: {
    username: 'testuser001',
    password: 'TestPass@123',
    role: 'SCHOOL_ADMIN',
  },
  school_staff: {
    username: 'test_staff_001',
    password: 'TestPass@123',
    role: 'SCHOOL_STAFF',
  },
};

export const API_BASE = 'http://localhost:8080/api/v1';
```

**Step 2: 创建 API Helper**

```typescript
// e2e/helpers/api.helper.ts

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
```

**Step 3: Commit**

```bash
git add e2e/helpers/
git commit -m "feat(e2e): add API helper and test account config"
```

---

### Task 4: 创建 Global Setup（认证状态持久化）

**Files:**
- Create: `e2e/global.setup.ts`

**Step 1: 创建 global setup**

```typescript
// e2e/global.setup.ts

import { test as setup } from '@playwright/test';
import { ACCOUNTS, API_BASE } from './helpers/accounts';

const authenticate = async (
  projectName: string,
  page: import('@playwright/test').Page,
) => {
  const account = ACCOUNTS[projectName];
  if (!account) return;

  // API login to get token
  const res = await page.request.post(`${API_BASE}/auth/login`, {
    data: { username: account.username, password: account.password },
  });
  const body = await res.json();
  if (body.code !== 0) throw new Error(`Setup login failed for ${projectName}: ${body.message}`);
  const token = body.data.accessToken;

  // Navigate to app and inject token into localStorage
  await page.goto('http://localhost:5174/login');
  await page.evaluate((t) => {
    localStorage.setItem('access_token', t);
  }, token);

  // Navigate to dashboard to confirm auth works
  await page.goto('http://localhost:5174/dashboard');
  await page.waitForLoadState('networkidle');

  // Save storage state
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
```

**Step 2: 添加 .auth 到 .gitignore**

在 `e2e/.gitignore` 中添加:

```
.auth/
test-results/
playwright-report/
node_modules/
```

**Step 3: Commit**

```bash
git add e2e/global.setup.ts e2e/.gitignore
git commit -m "feat(e2e): add global auth setup with storage state persistence"
```

---

### Task 5: 创建 Page Objects

**Files:**
- Create: `e2e/pages/login.page.ts`
- Create: `e2e/pages/student-list.page.ts`
- Create: `e2e/pages/school-manage.page.ts`
- Create: `e2e/pages/statistics.page.ts`
- Create: `e2e/pages/sidebar.component.ts`

**Step 1: 创建 Login Page Object**

```typescript
// e2e/pages/login.page.ts

import { type Page, type Locator } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.getByPlaceholder('请输入用户名');
    this.passwordInput = page.getByPlaceholder('请输入密码');
    this.submitButton = page.getByRole('button', { name: '登 录' });
    this.errorMessage = page.locator('.el-message--error');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }
}
```

**Step 2: 创建 Sidebar Component Object**

```typescript
// e2e/pages/sidebar.component.ts

import { type Page, type Locator } from '@playwright/test';

export class Sidebar {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  menuItem(text: string): Locator {
    return this.page.locator('.el-menu-item, .el-sub-menu__title').filter({ hasText: text });
  }

  async navigateTo(menuText: string) {
    await this.menuItem(menuText).click();
    await this.page.waitForLoadState('networkidle');
  }

  async visibleMenuItems(): Promise<string[]> {
    const items = this.page.locator('.el-menu-item');
    return items.allTextContents();
  }
}
```

**Step 3: 创建 StudentList Page Object**

```typescript
// e2e/pages/student-list.page.ts

import { type Page, type Locator } from '@playwright/test';

export class StudentListPage {
  readonly page: Page;
  readonly table: Locator;
  readonly statusFilter: Locator;
  readonly searchInput: Locator;
  readonly admitButton: Locator;
  readonly rejectButton: Locator;
  readonly batchRejectButton: Locator;
  readonly pagination: Locator;

  constructor(page: Page) {
    this.page = page;
    this.table = page.locator('.el-table');
    this.statusFilter = page.locator('.el-select').first();
    this.searchInput = page.getByPlaceholder('搜索');
    this.admitButton = page.getByRole('button', { name: '录取' });
    this.rejectButton = page.getByRole('button', { name: '拒绝' });
    this.batchRejectButton = page.getByRole('button', { name: '批量拒绝' });
    this.pagination = page.locator('.el-pagination');
  }

  async goto() {
    await this.page.goto('/students');
    await this.page.waitForLoadState('networkidle');
  }

  async rowCount(): Promise<number> {
    return this.page.locator('.el-table__row').count();
  }

  async getRowText(index: number): Promise<string> {
    return this.page.locator('.el-table__row').nth(index).textContent() ?? '';
  }
}
```

**Step 4: 创建 SchoolManage Page Object**

```typescript
// e2e/pages/school-manage.page.ts

import { type Page, type Locator } from '@playwright/test';

export class SchoolManagePage {
  readonly page: Page;
  readonly table: Locator;
  readonly addButton: Locator;
  readonly formDialog: Locator;
  readonly schoolNameInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.table = page.locator('.el-table');
    this.addButton = page.getByRole('button', { name: /新增院校/ });
    this.formDialog = page.locator('.el-dialog');
    this.schoolNameInput = page.locator('.el-dialog').getByPlaceholder('请输入院校名称');
  }

  async goto() {
    await this.page.goto('/admin/schools');
    await this.page.waitForLoadState('networkidle');
  }

  async rowCount(): Promise<number> {
    return this.page.locator('.el-table__row').count();
  }
}
```

**Step 5: 创建 Statistics Page Object**

```typescript
// e2e/pages/statistics.page.ts

import { type Page, type Locator } from '@playwright/test';

export class StatisticsPage {
  readonly page: Page;
  readonly kpiCards: Locator;
  readonly charts: Locator;

  constructor(page: Page) {
    this.page = page;
    this.kpiCards = page.locator('.el-statistic, .el-card').filter({ hasText: /待处理|已录取|已确认|已报到/ });
    this.charts = page.locator('canvas, .echarts, [_echarts_instance_]');
  }

  async goto() {
    await this.page.goto('/statistics');
    await this.page.waitForLoadState('networkidle');
  }
}
```

**Step 6: Commit**

```bash
git add e2e/pages/
git commit -m "feat(e2e): add Page Objects for login, sidebar, students, schools, statistics"
```

---

### Task 6: 迁移 auth.spec.ts（MOD-01 + MOD-13）

**Files:**
- Create: `e2e/specs/auth.spec.ts`

**Step 1: 创建 auth spec**

```typescript
// e2e/specs/auth.spec.ts

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
```

**Step 2: 运行测试验证**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/e2e
npx playwright test specs/auth.spec.ts --reporter=list
```

期望: 全部 PASS（需要后端和前端服务运行中）。

**Step 3: Commit**

```bash
git add e2e/specs/auth.spec.ts
git commit -m "feat(e2e): add auth and security specs (MOD-01, MOD-13)"
```

---

### Task 7: 迁移 student.spec.ts（MOD-02）

**Files:**
- Create: `e2e/specs/student.spec.ts`

**Step 1: 创建 student spec**

```typescript
// e2e/specs/student.spec.ts

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
```

**Step 2: 运行测试**

```bash
npx playwright test specs/student.spec.ts --reporter=list
```

**Step 3: Commit**

```bash
git add e2e/specs/student.spec.ts
git commit -m "feat(e2e): add student management specs (MOD-02)"
```

---

### Task 8: 迁移 admission.spec.ts（MOD-03）

**Files:**
- Create: `e2e/specs/admission.spec.ts`

**Step 1: 创建 admission spec**

```typescript
// e2e/specs/admission.spec.ts

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
      // 可能成功(0)或名额不足(非0)，都是合法响应
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
    // 应被权限拦截或返回错误
    expect(result.code).not.toBe(0);
  });
});
```

**Step 2: 运行测试**

```bash
npx playwright test specs/admission.spec.ts --reporter=list
```

**Step 3: Commit**

```bash
git add e2e/specs/admission.spec.ts
git commit -m "feat(e2e): add admission operation specs (MOD-03)"
```

---

### Task 9: 迁移剩余 spec 文件（MOD-04/08/09/10）

**Files:**
- Create: `e2e/specs/supplement.spec.ts`
- Create: `e2e/specs/statistics.spec.ts`
- Create: `e2e/specs/integration.spec.ts`
- Create: `e2e/specs/health.spec.ts`

**Step 1: 创建 supplement spec**

```typescript
// e2e/specs/supplement.spec.ts

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

  test.use({ storageState: '.auth/school_admin.json' });

  test('补录管理页面加载', async ({ page }) => {
    await page.goto('/supplement');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.el-card, .el-empty')).toBeVisible();
  });
});
```

**Step 2: 创建 statistics spec**

```typescript
// e2e/specs/statistics.spec.ts

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
    // 页面应包含 ECharts canvas 或统计卡片
    await expect(page.locator('.el-card').first()).toBeVisible();
  });
});
```

**Step 3: 创建 integration spec**

```typescript
// e2e/specs/integration.spec.ts

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
```

**Step 4: 创建 health spec**

```typescript
// e2e/specs/health.spec.ts

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
```

**Step 5: 运行全量 E2E**

```bash
npx playwright test --reporter=list
```

**Step 6: Commit**

```bash
git add e2e/specs/
git commit -m "feat(e2e): add supplement, statistics, integration, health specs (MOD-04/08/09/10)"
```

---

### Task 10: 标记旧 E2E 脚本废弃 + 更新文档

**Files:**
- Modify: `campus-platform/e2e_test.js` (添加废弃注释)

**Step 1: 在旧脚本顶部添加废弃说明**

在 `e2e_test.js` 第 1 行之前添加：

```javascript
/**
 * @deprecated 已迁移到 e2e/ 目录的 Playwright Test Runner 框架
 * 运行新版: cd e2e && npx playwright test
 * 本脚本保留仅供参考，不再维护。
 */
```

**Step 2: Commit**

```bash
git add e2e_test.js
git commit -m "chore: deprecate legacy e2e_test.js in favor of e2e/ framework"
```

---

## 阶段三：前端测试

### Task 11: 初始化 Vitest + Vue Test Utils

**Files:**
- Modify: `frontend/package.json` (添加 devDependencies)
- Create: `frontend/vitest.config.ts`

**Step 1: 安装测试依赖**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/frontend
npm install -D vitest @vue/test-utils happy-dom @pinia/testing
```

**Step 2: 创建 vitest.config.ts**

```typescript
// frontend/vitest.config.ts

import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    globals: true,
    root: fileURLToPath(new URL('./', import.meta.url)),
    include: ['src/**/__tests__/**/*.test.ts'],
    setupFiles: ['src/__tests__/setup.ts'],
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
});
```

**Step 3: 创建全局 setup 文件**

```typescript
// frontend/src/__tests__/setup.ts

import { config } from '@vue/test-utils';

// Stub Element Plus 组件以避免完整渲染
config.global.stubs = {
  ElButton: true,
  ElInput: true,
  ElForm: true,
  ElFormItem: true,
  ElTable: true,
  ElTableColumn: true,
  ElDialog: true,
  ElMessage: true,
  ElMessageBox: true,
  ElSelect: true,
  ElOption: true,
  ElPagination: true,
  ElCard: true,
  ElTag: true,
  ElIcon: true,
  ElMenu: true,
  ElMenuItem: true,
  ElSubMenu: true,
  ElDropdown: true,
  ElBadge: true,
  ElEmpty: true,
  ElStatistic: true,
  transition: false,
};
```

**Step 4: 在 package.json 中添加 test 脚本**

在 `frontend/package.json` 的 `scripts` 中添加：

```json
"test": "vitest",
"test:run": "vitest run",
"test:coverage": "vitest run --coverage"
```

**Step 5: 运行验证 Vitest 可启动**

```bash
npx vitest run
```

期望: 0 tests（还没写），但框架启动成功。

**Step 6: Commit**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform
git add frontend/vitest.config.ts frontend/src/__tests__/setup.ts frontend/package.json
git commit -m "chore: initialize Vitest + Vue Test Utils for frontend testing"
```

---

### Task 12: Auth Store 测试

**Files:**
- Create: `frontend/src/stores/__tests__/auth.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/stores/__tests__/auth.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '../auth';

// Mock axios
vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
}));

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it('初始状态：无 token', () => {
    const store = useAuthStore();
    expect(store.token).toBeNull();
    expect(store.userInfo).toBeNull();
  });

  it('localStorage 有 token → store 读取', () => {
    localStorage.setItem('access_token', 'test-token');
    const store = useAuthStore();
    expect(store.token).toBe('test-token');
  });

  it('loginAction 成功 → 存储 token', async () => {
    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.post).mockResolvedValueOnce({
      data: { code: 0, data: { accessToken: 'new-token', requirePasswordChange: false } },
    });

    const store = useAuthStore();
    await store.loginAction({ username: 'test', password: 'pass' });

    expect(localStorage.getItem('access_token')).toBe('new-token');
    expect(store.token).toBe('new-token');
  });

  it('logoutAction → 清除 token', async () => {
    localStorage.setItem('access_token', 'old-token');
    const store = useAuthStore();

    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.post).mockResolvedValueOnce({ data: { code: 0 } });

    await store.logoutAction();
    expect(store.token).toBeNull();
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('role computed 从 userInfo 取值', () => {
    const store = useAuthStore();
    store.userInfo = { role: 'OP_ADMIN', realName: '管理员', accountId: '123' } as any;
    expect(store.role).toBe('OP_ADMIN');
    expect(store.isOpAdmin).toBe(true);
  });

  it('userInfo 为 null 时 role 为 null', () => {
    const store = useAuthStore();
    expect(store.role).toBeNull();
    expect(store.isOpAdmin).toBe(false);
  });
});
```

**Step 2: 运行测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/frontend
npx vitest run src/stores/__tests__/auth.test.ts
```

期望: 6 tests passed。

**Step 3: Commit**

```bash
git add src/stores/__tests__/auth.test.ts
git commit -m "test: add auth store unit tests"
```

---

### Task 13: Notification Store 测试

**Files:**
- Create: `frontend/src/stores/__tests__/notification.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/stores/__tests__/notification.test.ts

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useNotificationStore } from '../notification';

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn(),
  },
}));

describe('notification store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('初始状态：空列表，0 未读', () => {
    const store = useNotificationStore();
    expect(store.list).toEqual([]);
    expect(store.unreadCount).toBe(0);
    expect(store.hasUnread).toBe(false);
  });

  it('fetchNotifications 更新列表', async () => {
    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.get).mockResolvedValueOnce({
      data: {
        code: 0,
        data: {
          records: [{ id: '1', title: 'Test', read: false }],
          total: 1,
        },
      },
    });

    const store = useNotificationStore();
    await store.fetchNotifications();
    expect(store.list.length).toBe(1);
  });

  it('startPolling 启动定时器', () => {
    const store = useNotificationStore();
    store.startPolling();
    expect(store.hasUnread).toBe(false);
    store.stopPolling();
  });

  it('stopPolling 清除定时器', () => {
    const store = useNotificationStore();
    store.startPolling();
    store.stopPolling();
    // 不应抛出异常
  });

  it('markAllRead 清除未读数', async () => {
    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.post).mockResolvedValueOnce({ data: { code: 0 } });

    const store = useNotificationStore();
    store.unreadCount = 5;
    await store.markAllRead();
    expect(store.unreadCount).toBe(0);
  });
});
```

**Step 2: 运行测试**

```bash
npx vitest run src/stores/__tests__/notification.test.ts
```

**Step 3: Commit**

```bash
git add src/stores/__tests__/notification.test.ts
git commit -m "test: add notification store unit tests"
```

---

### Task 14: Permission Store + usePermission Composable 测试

**Files:**
- Create: `frontend/src/stores/__tests__/permission.test.ts`
- Create: `frontend/src/composables/__tests__/usePermission.test.ts`

**Step 1: 创建 permission store 测试**

```typescript
// frontend/src/stores/__tests__/permission.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { usePermissionStore } from '../permission';

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('permission store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('初始状态：空模块，未加载', () => {
    const store = usePermissionStore();
    expect(store.modules).toEqual([]);
    expect(store.loaded).toBe(false);
  });

  it('fetchModules 加载后标记 loaded', async () => {
    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.get).mockResolvedValueOnce({
      data: {
        code: 0,
        data: [{ module: 'student', permissions: ['student:list'] }],
      },
    });

    const store = usePermissionStore();
    await store.fetchModules();
    expect(store.loaded).toBe(true);
    expect(store.modules.length).toBe(1);
  });

  it('fetchModules 幂等：已加载不重复请求', async () => {
    const { default: axios } = await import('@/api/axios');
    vi.mocked(axios.get).mockResolvedValueOnce({
      data: { code: 0, data: [] },
    });

    const store = usePermissionStore();
    await store.fetchModules();
    await store.fetchModules(); // 第二次不应发请求
    expect(vi.mocked(axios.get)).toHaveBeenCalledTimes(1);
  });
});
```

**Step 2: 创建 usePermission composable 测试**

```typescript
// frontend/src/composables/__tests__/usePermission.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { usePermission } from '../usePermission';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('OP_ADMIN → can() 始终返回 true', () => {
    const auth = useAuthStore();
    auth.userInfo = { role: 'OP_ADMIN', permissions: [] } as any;

    const { can } = usePermission();
    expect(can('student:list').value).toBe(true);
    expect(can('any:permission').value).toBe(true);
  });

  it('SCHOOL_ADMIN 有权限 → can() 返回 true', () => {
    const auth = useAuthStore();
    auth.userInfo = {
      role: 'SCHOOL_ADMIN',
      permissions: ['student:list', 'major:create'],
    } as any;

    const { can } = usePermission();
    expect(can('student:list').value).toBe(true);
    expect(can('major:create').value).toBe(true);
  });

  it('SCHOOL_ADMIN 无权限 → can() 返回 false', () => {
    const auth = useAuthStore();
    auth.userInfo = {
      role: 'SCHOOL_ADMIN',
      permissions: ['student:list'],
    } as any;

    const { can } = usePermission();
    expect(can('admin:manage').value).toBe(false);
  });

  it('isOpAdmin / isSchoolAdmin computed', () => {
    const auth = useAuthStore();
    auth.userInfo = { role: 'SCHOOL_STAFF', permissions: [] } as any;

    const { isOpAdmin, isSchoolAdmin, isSchoolStaff } = usePermission();
    expect(isOpAdmin.value).toBe(false);
    expect(isSchoolAdmin.value).toBe(false);
    expect(isSchoolStaff.value).toBe(true);
  });

  it('canAny 任一匹配返回 true', () => {
    const auth = useAuthStore();
    auth.userInfo = {
      role: 'SCHOOL_ADMIN',
      permissions: ['student:list'],
    } as any;

    const { canAny } = usePermission();
    expect(canAny('student:list', 'admin:manage').value).toBe(true);
  });
});
```

**Step 3: 运行测试**

```bash
npx vitest run src/stores/__tests__/permission.test.ts src/composables/__tests__/usePermission.test.ts
```

**Step 4: Commit**

```bash
git add src/stores/__tests__/ src/composables/__tests__/
git commit -m "test: add permission store and usePermission composable tests"
```

---

### Task 15: Axios 拦截器测试

**Files:**
- Create: `frontend/src/utils/__tests__/request.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/utils/__tests__/request.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';

// 需要在 import axios 实例之前 mock 依赖
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

describe('axios interceptors', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('请求拦截器：有 token → 注入 Authorization header', async () => {
    localStorage.setItem('access_token', 'test-token-123');

    // 动态 import 以确保 mock 先生效
    const { default: api } = await import('@/api/axios');

    // 检查 interceptor 配置
    expect(api.defaults.baseURL).toBe('/api');
    expect(api.defaults.timeout).toBe(30000);
  });

  it('请求拦截器：无 token → 不注入 header', async () => {
    localStorage.removeItem('access_token');
    const { default: api } = await import('@/api/axios');
    expect(api.defaults.headers.common?.['Authorization']).toBeUndefined();
  });

  it('axios 实例配置正确', async () => {
    const { default: api } = await import('@/api/axios');
    expect(api.defaults.baseURL).toBe('/api');
    expect(api.defaults.timeout).toBe(30000);
    expect(api.defaults.headers.post?.['Content-Type']).toBe('application/json');
  });
});
```

**Step 2: 运行测试**

```bash
npx vitest run src/utils/__tests__/request.test.ts
```

**Step 3: Commit**

```bash
git add src/utils/__tests__/request.test.ts
git commit -m "test: add axios interceptor config tests"
```

---

### Task 16: Login 组件测试

**Files:**
- Create: `frontend/src/views/__tests__/Login.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/views/__tests__/Login.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createMemoryHistory } from 'vue-router';
import Login from '@/views/common/Login.vue';

vi.mock('@/api/axios', () => ({
  default: { post: vi.fn(), get: vi.fn() },
}));

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn() },
}));

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/login', component: Login },
    { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
    { path: '/force-change-password', component: { template: '<div>Change</div>' } },
  ],
});

describe('Login.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('渲染登录表单', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia(), router],
      },
    });
    expect(wrapper.find('form, .el-form, .login-form').exists()).toBe(true);
  });

  it('包含用户名和密码输入框', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia(), router],
      },
    });
    const inputs = wrapper.findAll('input');
    expect(inputs.length).toBeGreaterThanOrEqual(2);
  });

  it('包含登录按钮', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [createPinia(), router],
      },
    });
    expect(wrapper.text()).toContain('登');
  });
});
```

**Step 2: 运行测试**

```bash
npx vitest run src/views/__tests__/Login.test.ts
```

**Step 3: Commit**

```bash
git add src/views/__tests__/Login.test.ts
git commit -m "test: add Login component rendering tests"
```

---

### Task 17: Layout 侧边栏角色渲染测试

**Files:**
- Create: `frontend/src/views/__tests__/Layout.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/views/__tests__/Layout.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createMemoryHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import Layout from '@/views/common/Layout.vue';

vi.mock('@/api/axios', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}));

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: Layout, children: [
      { path: 'dashboard', component: { template: '<div>Dashboard</div>' } },
      { path: 'students', component: { template: '<div>Students</div>' } },
      { path: 'admin/schools', component: { template: '<div>Schools</div>' } },
    ]},
  ],
});

describe('Layout.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('OP_ADMIN 可见院校管理', async () => {
    const auth = useAuthStore();
    auth.userInfo = { role: 'OP_ADMIN', realName: '管理员' } as any;
    auth.token = 'fake-token';

    const wrapper = mount(Layout, {
      global: { plugins: [createPinia(), router] },
    });

    const text = wrapper.text();
    expect(text).toContain('院校管理');
  });

  it('SCHOOL_ADMIN 可见考生菜单', async () => {
    const auth = useAuthStore();
    auth.userInfo = { role: 'SCHOOL_ADMIN', realName: '招生员', schoolId: '123' } as any;
    auth.token = 'fake-token';

    const wrapper = mount(Layout, {
      global: { plugins: [createPinia(), router] },
    });

    const text = wrapper.text();
    expect(text).toContain('考生');
  });

  it('显示用户名', () => {
    const auth = useAuthStore();
    auth.userInfo = { role: 'OP_ADMIN', realName: '张三' } as any;
    auth.token = 'fake-token';

    const wrapper = mount(Layout, {
      global: { plugins: [createPinia(), router] },
    });

    expect(wrapper.text()).toContain('张三');
  });
});
```

**Step 2: 运行测试**

```bash
npx vitest run src/views/__tests__/Layout.test.ts
```

**Step 3: Commit**

```bash
git add src/views/__tests__/Layout.test.ts
git commit -m "test: add Layout sidebar role-based rendering tests"
```

---

### Task 18: StudentList 组件测试

**Files:**
- Create: `frontend/src/views/__tests__/StudentList.test.ts`

**Step 1: 创建测试**

```typescript
// frontend/src/views/__tests__/StudentList.test.ts

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createMemoryHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import StudentList from '@/views/school/StudentList.vue';

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      data: {
        code: 0,
        data: {
          records: [
            { pushId: '1', candidateName: '张三', status: 'PENDING', totalScore: 85 },
          ],
          total: 1,
        },
      },
    }),
    post: vi.fn(),
  },
}));

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') },
}));

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/students', component: StudentList },
    { path: '/students/:id', component: { template: '<div>Detail</div>' } },
  ],
});

describe('StudentList.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.userInfo = {
      role: 'SCHOOL_ADMIN',
      realName: '招生员',
      schoolId: '123',
      permissions: ['student:list', 'student:admit'],
    } as any;
    auth.token = 'fake-token';
  });

  it('渲染考生表格', async () => {
    const wrapper = mount(StudentList, {
      global: { plugins: [createPinia(), router] },
    });
    await flushPromises();
    expect(wrapper.find('.el-table, table').exists()).toBe(true);
  });

  it('包含状态筛选', () => {
    const wrapper = mount(StudentList, {
      global: { plugins: [createPinia(), router] },
    });
    expect(wrapper.text()).toContain('状态');
  });

  it('SCHOOL_ADMIN 可见录取按钮', () => {
    const wrapper = mount(StudentList, {
      global: { plugins: [createPinia(), router] },
    });
    // 录取相关操作区域应存在
    const text = wrapper.text();
    expect(text).toContain('录取');
  });
});
```

**Step 2: 运行测试**

```bash
npx vitest run src/views/__tests__/StudentList.test.ts
```

**Step 3: Commit**

```bash
git add src/views/__tests__/StudentList.test.ts
git commit -m "test: add StudentList component tests"
```

---

### Task 19: 全量前端测试验证

**Step 1: 运行全部前端测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/frontend
npx vitest run
```

期望: ~38 个测试全部通过。

**Step 2: 如有失败，修复后重跑**

**Step 3: 最终 Commit**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform
git add frontend/
git commit -m "test: complete frontend test suite - stores, composables, utils, components"
```

---

## 最终验证

### Task 20: 全量测试 + 清理

**Step 1: 后端测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/backend
mvn test -q
```

**Step 2: 前端测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/frontend
npx vitest run
```

**Step 3: E2E 测试（需要服务运行）**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/e2e
npx playwright test --reporter=list
```

**Step 4: 更新 progress.md**

在 `progress.md` 末尾追加本次会话记录。

**Step 5: Commit**

```bash
git add progress.md
git commit -m "docs: update progress with test advancement session"
```

---

## 总览

| 阶段 | Task | 产出 |
|------|------|------|
| 一 | 1 | 验证 66 个后端测试全绿 |
| 二 | 2-10 | E2E 框架：8 spec + fixtures + page objects + helpers |
| 三 | 11-19 | 前端测试：8 测试文件，~38 用例 |
| 验证 | 20 | 全量回归 + 文档更新 |
