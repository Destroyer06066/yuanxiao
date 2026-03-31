# 院校管理平台 — 产品优化设计方案

> 日期：2026-03-27
> 状态：✅ 已全部实施（2026-03-27）

---

## 背景

平台已完成 Phase 0-6 基础功能开发，核心招录流程可跑通。但从业务实操角度审视，存在多处不合理：OP_ADMIN 看不到考生数据、考生详情页过于简陋、业务流程在页面间断裂、关键操作缺少闭环。本方案覆盖所有已识别问题，按模块组织，按优先级排序。

---

## 模块一：考生信息体系重构（P0）

### 1.1 OP_ADMIN 开放考生列表（只读）

**问题**：路由守卫把考生列表/详情限制为 SCHOOL_ADMIN/STAFF，OP_ADMIN 作为全局管理者无法查看任何考生数据。

**方案**：
- 路由 meta.roles 增加 `OP_ADMIN`：`/students`、`/students/:id`
- 后端 `StudentController` 的 `@RequireRole` 增加 `OP_ADMIN`
- OP_ADMIN 的考生列表增加"推送院校"列，支持按院校下拉筛选
- OP_ADMIN 在列表和详情页**不显示操作按钮**（不能录取/拒绝，因为不代表任何学校）
- 后端数据隔离逻辑已支持：OP_ADMIN 时 schoolId=null，查询全部

**改动范围**：
- `frontend/src/router/index.ts`：2 条路由 roles 增加 OP_ADMIN
- `StudentList.vue`：增加院校列 + 院校筛选下拉 + v-if 隐藏操作按钮
- `StudentDetail.vue`：v-if 隐藏操作面板
- `StudentController.java`：@RequireRole 增加 OP_ADMIN

### 1.2 考生详情页完善

**问题**：当前只显示 5 个字段，无分科成绩、无操作历史、无录取操作入口。

**方案**：

页面分为 4 个区域：

**区域 A — 基本信息卡片**
- 左侧：姓名、国籍、证件号、邮箱、意向方向、推送院校、推送时间
- 右侧：状态标签（大号彩色 Tag）

**区域 B — 分科成绩表格**
- 从 `subjectScores` JSON 展开为表格：科目 | 分数
- 末尾显示总分（加粗）

**区域 C — 录取信息（当 status != PENDING 时显示）**
- 录取专业、录取备注
- 条件描述、条件截止日期（仅 CONDITIONAL 显示）
- 操作人、操作时间

**区域 D — 操作时间线**
- 纵向时间轴，每个节点显示：时间、操作类型、操作人
- 数据来源：新增 `operation_log` 表

**区域 E — 操作面板（仅 SCHOOL 角色 + 可操作状态时显示）**
- PENDING 状态：[直接录取] [有条件录取] [拒绝]
- CONDITIONAL 状态：[确认条件满足→转正式录取] [撤销条件录取]
- ADMITTED 状态：显示"等待考生确认"
- CONFIRMED 状态：[登记材料收件] — 直接在详情页操作，不必跳转报到管理
- MATERIAL_RECEIVED 状态：[确认报到]

这样一个详情页就能覆盖考生的完整生命周期，不再需要在多个页面之间跳转。

**新增后端资源**：
- `operation_log` 表：`log_id, push_id, action, operator_id, operator_name, remark, created_at`
- 在所有状态变更操作中写入 operation_log
- 新增 API：`GET /api/v1/students/{pushId}/timeline` 返回时间线数据

### 1.3 全局搜索

**问题**：没有快速定位考生的方式。

**方案**：
- 顶部导航栏增加搜索框（Layout.vue header 区域）
- 支持按姓名、证件号、候选人 ID 模糊搜索
- 搜索结果下拉展示最多 10 条匹配：姓名 + 院校 + 状态
- 点击跳转 `/students/:pushId`
- OP_ADMIN 搜全部，SCHOOL 角色只搜本校
- 新增 API：`GET /api/v1/students/search?keyword=xxx`

---

## 模块二：Dashboard 差异化（P1）

### 2.1 OP_ADMIN Dashboard

**问题**：所有角色看到一样的 4 个 KPI 数字，对 OP_ADMIN 来说信息密度太低。

**方案**：

OP_ADMIN 首页包含：

**第一行 — 全局 KPI（保留现有 4 个指标）**

**第二行 — 各校录取进度排行**
- 表格：院校名称 | 推送人数 | 已录取 | 已确认 | 已报到 | 录取率
- 按录取率排序，可切换年份
- 新增 API：`GET /api/v1/statistics/school-progress`

**第三行 — 异常提醒卡片**
- 有条件录取即将到期（3 天内）：X 人
- 名额使用超 90% 的专业：X 个
- 今日新推送考生：X 人
- 每个卡片可点击跳转对应列表

**第四行 — 近期操作动态**
- 最近 20 条全局操作记录（来自 operation_log）
- 格式：`[时间] [院校] [操作人] 对 [考生] 执行了 [操作]`

### 2.2 SCHOOL_ADMIN/STAFF Dashboard

**方案**：

**第一行 — 本校 KPI（保留现有）**

**第二行 — 待办事项**
- 待处理考生（PENDING）：X 人 → 点击跳转考生列表（筛选 PENDING）
- 有条件录取即将到期：X 人 → 点击跳转考生列表（筛选 CONDITIONAL + 排序截止日期）
- 待材料收件（CONFIRMED）：X 人 → 点击跳转报到管理
- 待确认报到（MATERIAL_RECEIVED）：X 人

**第三行 — 名额使用概览**
- 横向柱状图：每个专业的名额使用率
- 红色标记超 90% 的专业

**第四行 — 本校近期操作**
- 最近 10 条操作记录

---

## 模块三：侧边栏菜单分组（P1）

**问题**：10 个菜单平铺，无逻辑分组。

**方案**：

OP_ADMIN 视角：
```
招生管理
  ├ 考生列表（新增）
  ├ 补录管理
  └ 数据统计

院校管理
  ├ 院校列表
  └ 账号管理

系统
  ├ 角色权限
  ├ 操作日志（新增）
  └ 站内通知
```

SCHOOL_ADMIN/STAFF 视角：
```
招生管理
  ├ 考生列表
  ├ 报到管理
  └ 补录管理

配置管理
  ├ 专业配置
  ├ 名额管理
  └ 分数线配置

数据
  ├ 数据统计
  └ 成绩核验

系统
  ├ 账号管理（仅 SCHOOL_ADMIN）
  └ 站内通知
```

实现方式：Layout.vue 侧边栏改为分组渲染，用 `el-menu` 的 `el-sub-menu` 分组。

---

## 模块四：有条件录取闭环（P1）

**问题**：设置条件后没有后续操作入口，无法确认"条件已满足"。

**方案**：

### 4.1 条件录取状态管理
- 考生详情页 CONDITIONAL 状态下新增操作按钮：[确认条件满足 → 转正式录取]
- 新增后端 API：`POST /api/v1/students/finalize` — 将 CONDITIONAL 转为 ADMITTED
- 操作时记录 operation_log

### 4.2 到期提醒
- 后端定时任务（已有 ConditionalExpiryTask）扫描到期：保持不变
- 新增：到期前 3 天生成站内通知，推送给对应院校的 SCHOOL_ADMIN
- Dashboard 待办事项中展示"即将到期"数量

### 4.3 条件录取专项视图
- 考生列表增加快捷筛选标签："有条件录取"，直接筛选 CONDITIONAL 状态
- 列表增加"条件截止日"列（仅 CONDITIONAL 状态显示）
- 按截止日排序，最紧急的排最前

---

## 模块五：页面互通与流程贯通（P2）

### 5.1 报到管理 ↔ 考生详情互通

**问题**：报到管理和考生列表是两个独立页面，同一考生信息割裂。

**方案**：
- 报到管理表格的考生姓名列加超链接，点击跳转 `/students/:pushId`
- 考生详情页中，CONFIRMED/MATERIAL_RECEIVED 状态直接提供操作按钮（如上 1.2 所述）
- 实际上，当详情页具备完整操作能力后，报到管理页面的定位变为"批量视图" —— 快速浏览所有待报到考生，点击进详情做操作

### 5.2 补录轮次关联考生

**问题**：补录轮次创建后，和考生数据脱节。

**方案**：
- 补录管理的每个轮次卡片增加统计数字：推送 X 人 / 录取 X 人 / 确认 X 人
- 点击轮次卡片 → 跳转考生列表，自动筛选 `pushRound = 该轮次`
- 考生列表增加"推送轮次"筛选项

### 5.3 名额管理 ↔ 录取操作联动

**问题**：录取弹窗选专业时看不到剩余名额。

**方案**：
- 录取弹窗的专业下拉选项中，每个专业后面显示"（剩余 X）"
- 剩余为 0 的专业标红，仍可选择但弹二次确认"该专业名额已满，确认继续？"

---

## 模块六：通知系统业务事件触发（P2）

**问题**：通知表有数据模型，但没有任何业务事件会生成通知。

**方案 — 自动生成通知的事件**：

| 事件 | 接收人 | 通知内容 |
|------|--------|---------|
| 新考生推送 | 该校 SCHOOL_ADMIN | "考生 [姓名] 已推送至贵校，总分 [X]" |
| 考生确认录取 | 该校 SCHOOL_ADMIN | "考生 [姓名] 已确认录取贵校 [专业]" |
| 有条件录取即将到期（3天） | 该校 SCHOOL_ADMIN | "考生 [姓名] 的条件录取将于 [日期] 到期" |
| 有条件录取已到期 | 该校 SCHOOL_ADMIN + OP_ADMIN | "考生 [姓名] 的条件录取已到期，已自动失效" |
| 名额使用超 90% | 该校 SCHOOL_ADMIN | "[专业] 名额使用已达 [X]%，剩余 [Y] 个" |
| 补录轮次开启 | 全部 SCHOOL_ADMIN | "第 [N] 轮补录已开启，截止 [日期]" |

实现方式：
- 新建 `NotificationService.createNotification(recipientId, title, content)`
- 在对应 Service 方法中调用（CandidateService、AdmissionService、ConditionalExpiryTask 等）
- NotificationStore 已有 30s 轮询，无需改动前端轮询逻辑

---

## 模块七：操作审计日志页面（P3）

**问题**：后端有 AuditLogAspect 但前端无查看入口。

**方案**：
- 新增页面 `/admin/audit-logs`，仅 OP_ADMIN 可访问
- 表格列：时间 | 操作人 | 角色 | 院校 | 操作类型 | 目标对象 | IP
- 支持按时间范围、操作类型、操作人筛选
- 新增后端 API：`GET /api/v1/admin/audit-logs`（分页）

---

## 模块八：导出功能（P3）

**问题**：导出按钮存在但无实际逻辑。

**方案**：
- 考生列表页"导出"按钮：按当前筛选条件导出 Excel
- 后端新增：`GET /api/v1/students/export?status=...&schoolId=...` 返回 xlsx 文件流
- 使用 Apache POI 生成 Excel
- 导出字段：姓名、国籍、证件号、总分、意向、状态、录取专业、推送时间、操作时间

---

## 模块九：名额预警（P3）

**问题**：名额管理只是静态表格，没有预警机制。

**方案**：
- 名额管理页面：剩余 ≤ 20% 的行标黄，剩余 = 0 的行标红
- Dashboard 名额概览（见模块二）
- 名额超 90% 自动生成通知（见模块六）

---

## 模块十：侧边栏调试代码清理

**问题**：Layout.vue 中补录管理的 v-if 有 `|| true` 调试残留。

**方案**：移除 `|| true`，恢复正确的角色判断逻辑。

---

## 数据库变更汇总

### 新增表

```sql
-- 操作日志（考生状态变更记录，用于时间线展示）
CREATE TABLE operation_log (
    log_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    push_id      UUID NOT NULL REFERENCES candidate_push(push_id),
    action       VARCHAR(50) NOT NULL,  -- PUSH, ADMIT, CONDITIONAL, REJECT, CONFIRM, MATERIAL_RECEIVE, CHECKIN, FINALIZE, INVALIDATE
    operator_id  UUID,
    operator_name VARCHAR(50),
    remark       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_oplog_push_id ON operation_log(push_id);
CREATE INDEX idx_oplog_created ON operation_log(created_at DESC);
```

### 现有表无需改动
- `notification` 表已存在，只需业务层写入
- `candidate_push` 表已有 conditionDesc、conditionDeadline 字段

---

## 新增 API 汇总

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/students/search?keyword=xxx` | 全局搜索 |
| GET | `/api/v1/students/{pushId}/timeline` | 考生操作时间线 |
| POST | `/api/v1/students/finalize` | 条件录取转正式录取 |
| GET | `/api/v1/statistics/school-progress` | 各校录取进度（OP_ADMIN） |
| GET | `/api/v1/statistics/alerts` | 异常提醒数据 |
| GET | `/api/v1/admin/audit-logs` | 审计日志查询 |
| GET | `/api/v1/students/export` | 考生列表导出 Excel |

---

## 实施阶段划分

### 第一阶段（P0）— 考生信息体系
1. OP_ADMIN 开放考生列表（路由 + 权限 + UI 适配）
2. 考生详情页完善（分科成绩、录取信息、操作面板）
3. operation_log 表 + 时间线 API + 详情页时间线组件
4. 在所有状态变更操作中写入 operation_log

### 第二阶段（P1）— 流程闭环
5. 有条件录取闭环（确认满足按钮 + finalize API + 到期提醒通知）
6. Dashboard 差异化（OP_ADMIN 版 + SCHOOL 版）
7. 侧边栏菜单分组
8. 全局搜索

### 第三阶段（P2）— 体验优化
9. 页面互通跳转（报到→详情、补录→考生列表）
10. 补录轮次关联统计
11. 录取弹窗显示剩余名额
12. 通知系统业务事件触发

### 第四阶段（P3）— 完善收尾
13. 审计日志页面
14. 导出功能
15. 名额预警
16. 调试代码清理
