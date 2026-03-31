# 院校管理平台 — 测试全面推进设计

> **日期**: 2026-03-28
> **目标**: 后端单测补全 + E2E 框架化 + 前端测试，本地可运行
> **方案**: 渐进式（阶段一 → 二 → 三），每阶段独立可用

---

## 阶段一：后端单测补全

执行现有计划 `2026-03-27-comprehensive-test-cases.md`，补充 29 个用例到 6 个测试文件。

| 测试文件 | 新增 | 覆盖点 |
|---------|------|--------|
| AccountServiceTest | +7 | createSchoolAdmin(3) + createStaff(4) |
| AdmissionServiceTest | +7 | conditionalAdmission 边界(3) + finalAdmission/reject/revoke(4) |
| CandidateServiceTest | +3 | receivePush 重复推送(PENDING/CONDITIONAL/CONFIRMED) |
| JwtTokenProviderTest | +9 | **新文件**，generate/parse/validate 全路径 |
| ConditionalExpiryTaskTest | +3 | **新文件**，定时任务空/批量/异常容忍 |

**验证**: 每个 Task 完成后 `mvn test` 确认通过，最终全量 ~62 个测试全绿。

---

## 阶段二：E2E 框架化

将 `e2e_test.js` 单脚本迁移到 Playwright Test Runner 正式框架。

### 目录结构

```
campus-platform/e2e/
├── playwright.config.ts        # baseURL、超时、重试、reporter
├── fixtures/
│   └── auth.fixture.ts         # 登录 fixture，提供已认证的 page/apiContext
├── pages/
│   ├── login.page.ts           # Page Object: 登录页
│   ├── student-list.page.ts    # Page Object: 考生列表
│   ├── school-manage.page.ts   # Page Object: 院校管理
│   └── statistics.page.ts      # Page Object: 统计仪表盘
├── specs/
│   ├── auth.spec.ts            # MOD-01: 登录/登出/权限拦截/无效Token
│   ├── student.spec.ts         # MOD-02: 考生列表/详情/数据隔离
│   ├── admission.spec.ts       # MOD-03: 录取/有条件录取/拒绝
│   ├── supplement.spec.ts      # MOD-04: 补录轮次
│   ├── statistics.spec.ts      # MOD-08: KPI/图表/院校进度
│   ├── integration.spec.ts     # MOD-09: Mock服务/推送
│   ├── health.spec.ts          # MOD-10: 健康检查
│   └── security.spec.ts        # MOD-13: 无效Token/越权访问
└── helpers/
    └── api.helper.ts           # API 工具函数（apiLogin/apiGet/apiPost）
```

### 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 语言 | TypeScript | 与前端一致，类型安全 |
| Page Object 模式 | 是 | 页面变动时只改一处 |
| Auth Fixture | 预登录注入 Token | 复用 localStorage 注入，避免重复登录 |
| 测试粒度 | 按模块拆分 spec | 对应 MOD-01 ~ MOD-13 |
| 并行策略 | 按角色 project 并行 | op_admin / school_admin / school_staff |
| Reporter | HTML + list | 本地可看报告 |

### 运行方式

```bash
npx playwright test                        # 全量
npx playwright test specs/auth.spec.ts     # 按模块
npx playwright test --project=op_admin     # 按角色
```

### 用例迁移

现有 `e2e_test.js` 的 ~25 个用例全部迁移到对应 spec 文件，旧脚本标记废弃。

---

## 阶段三：前端测试

引入 Vitest + Vue Test Utils + happy-dom。

### 目录结构

```
frontend/
├── vitest.config.ts
├── src/
│   ├── stores/__tests__/
│   │   ├── auth.test.ts
│   │   ├── notification.test.ts
│   │   └── permission.test.ts
│   ├── composables/__tests__/
│   │   └── usePermission.test.ts
│   ├── utils/__tests__/
│   │   └── request.test.ts
│   └── views/__tests__/
│       ├── Login.test.ts
│       ├── Layout.test.ts
│       └── StudentList.test.ts
```

### 覆盖范围

| 层级 | 测试内容 | 预计用例数 |
|------|---------|-----------|
| Store | auth、notification、permission | ~15 |
| Composable | usePermission 权限判断 | ~5 |
| Utils | axios 拦截器（Token注入、错误拦截、401跳转） | ~6 |
| 组件 | Login（表单校验）、Layout（角色菜单）、StudentList（表格/弹窗） | ~12 |
| **合计** | | **~38** |

### 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 测试运行器 | Vitest | 与 Vite 原生集成 |
| DOM 环境 | happy-dom | 比 jsdom 快 3-5 倍 |
| 组件测试策略 | 浅渲染 + mock API | 不启动后端 |
| Element Plus | stub 处理 | 避免完整 UI 库渲染开销 |
| 测试文件位置 | `__tests__/` 就近放置 | 改文件时顺手看测试 |

### 运行方式

```bash
cd frontend
npx vitest              # watch 模式
npx vitest run          # 单次运行
npx vitest --coverage   # 覆盖率报告
```

---

## 总览

| 阶段 | 产出 | 新增用例数 |
|------|------|-----------|
| 一：后端单测 | 6 个测试文件，62 个用例 | +29 |
| 二：E2E 框架化 | 8 个 spec + fixtures + page objects | ~25（迁移）+ 扩展 |
| 三：前端测试 | 8 个测试文件 | ~38 |
| **合计** | | **~130** |

**执行顺序**: 阶段一 → 阶段二 → 阶段三，每阶段完成后独立可用。
