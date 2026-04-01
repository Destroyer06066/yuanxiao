# 名额管理编辑弹窗重构设计

## 1. 背景与目标

将截止时间、开始时间、分数线全部收敛到"编辑名额"这一个弹窗中，消除弹窗外的独立列和独立分数线配置弹窗。

---

## 2. 数据库变更

新增 `start_time` 字段到 `admission_quota` 表：

```sql
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ;
COMMENT ON COLUMN admission_quota.start_time IS '录取开始时间';
```

---

## 3. 后端变更

### 3.1 Entity

`AdmissionQuota.java` 新增字段：

```java
@TableField("start_time")
private Instant startTime;
```

### 3.2 DTO 变更

- `QuotaRequest`（创建）：新增 `private Instant startTime;`
- `QuotaUpdateRequest`（更新）：新增 `private Instant startTime;`

### 3.3 Controller

- `GET /api/v1/quotas` 返回数据中增加 `startTime` 字段
- `POST /api/v1/quotas` 支持传入 `startTime`
- `PUT /api/v1/quotas/{quotaId}` 支持传入 `startTime`

---

## 4. 前端变更

### 4.1 移除内容

- 表格列：~~"录取截止时间"列~~
- 表格列：~~"分数线"列及"配置"按钮~~
- ~~单独的"分数线配置弹窗"~~
- `deadlineDraft` ref、相关 popover 和 datetime picker

### 4.2 编辑弹窗重构（核心）

**弹窗尺寸**：`width: "680px"`（适应更多字段）

**字段布局**：

```
专业           [国际关系（硕士）]     ← 只读
年份           [2026年]              ← 只读

录取时间段     [开始时间 DateTime] ~ [截止时间 DateTime]
               ↑可为空，表示不设开始限制

总名额         [ 80 ]

录取分数区间   [最低分 300] ~ [最高分 750]

────────── 分数线配置 ──────────
总分最低分:    [ 300 ]

单科线:
  科目         最低分
  语文         [  80  ]    [删除]
  数学         [  75  ]    [删除]
               [+ 添加单科]
```

**字段说明**：
- 专业+年份：编辑模式下只读，避免关联分数线错乱
- 开始时间：el-date-picker，可为空（`show-time`, `type="datetime"`）
- 截止时间：el-date-picker（`show-time`, `type="datetime"`）
- 总分最低分：el-input-number
- 单科线：动态表格，支持添加/删除科目行

**新建弹窗**：与编辑弹窗相同，但专业/年份可选择（去掉只读属性），专业下拉+年份下拉均可编辑。

### 4.3 数据获取

编辑弹窗打开时（`openEdit`），额外调用 `GET /api/v1/score-lines?majorId=xxx&year=xxx` 获取当前分数线，填入表单。

### 4.4 数据保存

**编辑模式下点击确定**：
1. 调用 `PUT /api/v1/quotas/{quotaId}` 保存 `startTime` + `deadline` + `totalQuota` + `minScore` + `maxScore`
2. 调用 `GET /api/v1/score-lines?majorId=xxx&year=xxx` 获取已有分数线
3. 过滤出 TOTAL 记录 → 存在则 PUT，不存在则 POST
4. 提交所有单科线 DELETE（不在列表中的旧记录）
5. 遍历新单科线 → 存在则 PUT，不存在则 POST

**新建模式下点击确定**：
1. 调用 `POST /api/v1/quotas` 创建名额（含 startTime + deadline）
2. 创建分数线（总分线 + 各单科线，POST）

---

## 5. 文件变更清单

| 文件 | 操作 |
|------|------|
| `backend/src/main/resources/db/migration/V18__add_start_time.sql` | 新建 |
| `backend/src/main/java/com/campus/platform/entity/AdmissionQuota.java` | 修改 |
| `backend/src/main/java/com/campus/platform/dto/QuotaRequest.java` | 修改 |
| `backend/src/main/java/com/campus/platform/dto/QuotaUpdateRequest.java` | 修改 |
| `backend/src/main/java/com/campus/platform/controller/QuotaController.java` | 修改 |
| `frontend/src/views/school/QuotaManage.vue` | 重构（移除独立列+弹窗，重构编辑弹窗） |

---

## 6. 实现顺序

1. **DB migration V18** — 新增 start_time 字段
2. **后端** — Entity + DTO + Controller 支持 startTime
3. **前端** — 重构编辑/新建弹窗，移除独立列和分数线弹窗
4. **浏览器测试** — 验证全部流程
