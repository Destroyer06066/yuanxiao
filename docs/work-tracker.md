# 院校管理平台 — 开发任务追踪

> 更新时间：2026-03-29（追加 Sprint 4）
> 项目路径：`/Volumes/HP P900/Tech/yuanxiao/campus-platform`

---

| Sprint 1 | 2026-03-29 | 4 个 P0 问题修复 | Login, Statistics, Checkin, QuotaManage |
| Sprint 2 | 2026-03-29 | 4 个 P1 问题修复 | ScoreLineConfig, SchoolManage, StudentList, CheckinManage |
| Sprint 3 | 2026-03-29 | 6 个 P1/P2 问题修复 | StudentList, Statistics, SchoolManage, AccountManage, RoleManage, RoleService, StatisticsService |
| Sprint 4 | 2026-03-29 | 5 个 P1/P2 问题修复 | StudentList, SupplementManage, Layout, Login, StudentDetail |

### Sprint 3 详细记录

| # | 问题 | 修复方案 | 文件 |
|---|------|----------|------|
| P1-6 | Dashboard KPI 趋势图 admitted/confirmed/checkedIn 全为 0 | 后端新增 `countByStatusYear()` 方法补充 `admittedLastYear` 等字段；前端接入真实 trend 值 | `StatisticsService.java`, `statistics.ts`, `Statistics.vue` |
| P2-1 | RoleManage 模块映射错误（补录在招生分组，缺 supplement 模块） | 后端 MODULE_LABELS 新增 `supplement` 条目；前端 MENU_ITEMS 将补录管理移入配置管理、module 改为 supplement；新增 V5 迁移脚本补充数据库权限记录 | `RoleService.java`, `RoleManage.vue`, `V5__add_supplement_permission.sql` |
| P2-2 | 密码重置未告知默认密码 | 提示文本改为"密码已重置为默认密码：Aa123456!" | `AccountManage.vue` |
| P2-3 | SchoolManage 表格缺少 `createdAt` 列 | 表格新增 `createdAt` 列 | `SchoolManage.vue` |
| P2-4 | StudentList 导出缺少 axios import（TS 编译错误） | 添加 `import axios from '@/api/axios'` | `StudentList.vue` |
| TD-Fix | `getMajorRanking` 测试 NPE、状态分布测试期望值错误 | 测试添加 `majorRepository` mock、更新 `MATERIAL_RECEIVED` 后状态数量断言（7→8） | `StatisticsServiceTest.java` |

### Sprint 4 详细记录

| # | 问题 | 修复方案 | 文件 |
|---|------|----------|------|
| P1-7 | 条件录取截止日应按紧迫程度排序 | 已实现：前端 `toggleQuickStatus('CONDITIONAL')` 切 `sort=conditionDeadline&order=ASC`；后端 mapper 支持 conditionDeadline 排序 | `StudentList.vue`, `CandidatePushMapper.xml` |
| P1-8 | 补录管理应支持批量操作 | 后端新增 `POST /v1/students/batch-admit` 端点；前端新增 `batchAdmit` API；考生列表新增批量录取按钮和对话框 | `StudentController.java`, `student.ts`, `StudentList.vue` |
| P2-5 | 考生详情操作面板按钮密度高 | PENDING 状态录取按钮改为按钮组合并，拒绝按钮独立，视觉更紧凑 | `StudentDetail.vue` |
| P2-6 | 全局搜索结果不显示分数 | 搜索下拉选项增加 `totalScore` 字段显示（蓝色标签） | `Layout.vue` |
| P2-7 | 登录页 logo 区域视觉待优化 | 头部新增 SVG 学校图标，增强品牌识别度 | `Login.vue` |

### Sprint 1 详细记录

| # | 问题 | 修复方案 | 文件 |
|---|------|----------|------|
| P0-1 | 忘记密码死链 | 注释未实现链接 | `frontend/src/views/common/Login.vue` |
| P0-2 | 专业排名显示 UUID | 后端补充 majorName 字段 | `backend/.../StatisticsService.java`, `api/statistics.ts`, `Statistics.vue` |
| P0-3 | 报到管理前端过滤破坏分页 | 改为后端参数筛选 | `CheckinController.java`, `CheckinService.java`, `api/checkin.ts`, `CheckinManage.vue` |
| P0-4 | 名额编辑缺少已占用信息 | 弹窗增加警告 + 前端校验 | `QuotaManage.vue` |

### Sprint 2 详细记录

| # | 问题 | 修复方案 | 文件 |
|---|------|----------|------|
| P1-1 | minScore 最大值 1000 应为 100 | `:max="1000"` → `:max="100"` | `ScoreLineConfig.vue` |
| P1-2 | schoolType 校验 trigger 不对 | `blur` → `change`, 提示改为"请选择" | `SchoolManage.vue` |
| P1-3 | 快捷标签统计仅当前页 | 改用后端 `/v1/statistics/status-dist` 全量统计 | `student.ts`, `StudentList.vue`, `StatisticsService.java` |
| P1-4 | 收件/报到对话框缺少备注 | 增加弹窗表单和提交逻辑 | `CheckinManage.vue`, `api/checkin.ts` |

---

## 二、待处理任务

### 🔴 P1 级（Sprint 4）

| 优先级 | 问题 | 来源 | 涉及文件 | 预计工时 | 备注 |
|--------|------|------|----------|----------|------|
| ~~P1-5~~ | ~~`enrolledCount` 列名错误~~ | UX报告 | — | — | **已核验：后端 `AdmissionQuota.admittedCount` 映射为 `enrolledCount`，前后端已对齐，非 bug** |
| ~~P1-6~~ | ~~Dashboard KPI 趋势图 trend 全为 0~~ | UX报告 | `StatisticsService.java`, `Statistics.vue` | — | **Sprint 3 已修复** |
| ~~P1-7~~ | ~~条件录取截止日应按紧迫程度排序~~ | UX报告 | `StudentList.vue`, `CandidatePushMapper.xml` | — | **Sprint 4 已修复** |
| ~~P1-8~~ | ~~补录管理应支持批量操作~~ | UX报告 | `StudentController.java`, `student.ts`, `StudentList.vue` | — | **Sprint 4 已修复** |

### 🟡 P2 级（Sprint 4/5）

| 优先级 | 问题 | 来源 | 涉及文件 | 预计工时 | 备注 |
|--------|------|------|----------|----------|------|
| ~~P2-1~~ | ~~RoleManage 模块映射错误~~ | UX报告 | `RoleManage.vue`, `RoleService.java` | — | **Sprint 3 已修复** |
| ~~P2-2~~ | ~~密码重置未告知默认密码~~ | UX报告 | `AccountManage.vue` | — | **Sprint 3 已修复** |
| ~~P2-3~~ | ~~SchoolManage 表格缺少 `createdAt` 列~~ | UX报告 | `SchoolManage.vue` | — | **Sprint 3 已修复** |
| ~~P2-4~~ | ~~导出缺少 axios import~~ | TS错误 | `StudentList.vue` | — | **Sprint 3 已修复** |
| ~~P2-5~~ | ~~考生详情操作面板可考虑精简~~ | UX报告 | `StudentDetail.vue` | — | **Sprint 4 已修复**：PENDING 录取按钮改为按钮组 |
| ~~P2-6~~ | ~~全局搜索结果不显示分数~~ | UX报告 | `Layout.vue` | — | **Sprint 4 已修复**：搜索下拉增加 totalScore 显示 |
| ~~P2-7~~ | ~~登录页 logo 区域视觉待优化~~ | UX报告 | `Login.vue` | — | **Sprint 4 已修复**：新增 SVG 学校图标 |
| ~~P2-8~~ | ~~专业管理列表缺少状态筛选~~ | UX报告 | `MajorConfig.vue` | — | **已核验：`MajorConfig.vue` 已有状态筛选（ACTIVE/INACTIVE），非 bug** |

---

## 三、技术债务

| # | 问题 | 文件 | 备注 |
|---|------|------|------|
| TD-1 | 前端 TypeScript 编译存在 10+ 既有错误（非本次修改引入） | 多个 .vue 文件 | 需逐个修复类型定义 |
| TD-2 | `PermissionModule` 类型与 API 返回值不匹配 | `stores/permission.ts` | 需对齐 Result<> 包装类型 |
| TD-3 | `.DS_Store` 和 `._*` 文件混入仓库 | 根目录多处 | 建议 .gitignore 清理 |

---

## 四、迭代计划

```
Sprint 1 ✅  2026-03-29  — P0 核心 Bug 修复（4项）
Sprint 2 ✅  2026-03-29  — P1 紧急体验优化（4项）
Sprint 3 ✅  2026-03-29  — P1-6 + P2-1~4 + 测试修复（6项）
Sprint 4 ✅  2026-03-29  — P1-7 + P1-8 + P2-5~7 + P2-8 核验（6项）
```

---

## 五、页面入口速查

| 页面 | 路径 | 角色可见 | 负责人 |
|------|------|----------|--------|
| 登录 | `/login` | 全部 | 前端 |
| 首页看板 | `/dashboard` | 全部 | 全栈 |
| 考生列表 | `/students` | 全部 | 全栈 |
| 考生详情 | `/students/:pushId` | 全部 | 全栈 |
| 报到管理 | `/checkin` | SCHOOL_ADMIN/STAFF | 全栈 |
| 专业配置 | `/majors` | SCHOOL_ADMIN/STAFF | 全栈 |
| 名额管理 | `/quota` | SCHOOL_ADMIN/STAFF | 全栈 |
| 分数线配置 | `/score-lines` | SCHOOL_ADMIN/STAFF | 全栈 |
| 补录管理 | `/supplement` | 全部 | 全栈 |
| 数据统计 | `/statistics` | 全部 | 全栈 |
| 成绩核验 | `/verification` | SCHOOL_ADMIN/STAFF | 全栈 |
| 账号管理 | `/accounts` | 全部（范围不同） | 全栈 |
| 角色管理 | `/roles` | OP_ADMIN | 全栈 |
| 院校管理 | `/admin/schools` | OP_ADMIN | 全栈 |
| 操作日志 | `/admin/audit-logs` | OP_ADMIN | 全栈 |
| 站内通知 | `/notifications` | 全部 | 全栈 |
