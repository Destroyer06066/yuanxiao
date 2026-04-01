# 招生简章与名额管理扩展功能设计

## 1. 背景与目标

本次迭代新增两个功能：

1. **招生简章配置页面**：让院校管理员编辑本校招生简章（标题 + 富文本内容）
2. **名额管理页面扩展**：新增录取截止时间设置 + 录取分数线（总分 + 单科）配置

---

## 2. 功能一：招生简章配置

### 2.1 数据库

复用已有 `school_brochure` 表，**无需新建表**：

```sql
-- V1__init_schema.sql 中已有结构
CREATE TABLE school_brochure (
    brochure_id  UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID       NOT NULL UNIQUE,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 2.2 后端

新建 `SchoolBrochureController` + `SchoolBrochureService`：

| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /api/v1/brochures` | GET | 获取本校简章 |
| `PUT /api/v1/brochures` | PUT | 新建/更新本校简章 |

- 查询时以 `school_id`（从 SecurityContext 获取）为条件，不暴露其他院校数据
- `school_id` 和 `status='DRAFT'` 由后端控制，前端不传

### 2.3 前端

- **路由**：`/brochure` → `SchoolBrochureConfig.vue`
- **入口**：院校侧菜单 → `配置管理` 分组 → `招生简章`
- **编辑器**：使用 Element Plus 的 `<el-input type="textarea">` 配合自定义富文本能力，或引入 `@vueup/vue-quill` 轻量富文本组件
- **只读模式**：无数据时显示"暂无简章，请点击编辑"；有数据时显示标题 + 内容预览 + 编辑按钮

---

## 3. 功能二：名额管理页面扩展

### 3.1 数据库变更

**V17__add_deadline_and_total_score.sql**

```sql
-- 新增 deadline 字段到 admission_quota 表
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS deadline TIMESTAMPTZ;

COMMENT ON COLUMN admission_quota.deadline IS '录取截止时间';
```

### 3.2 后端变更

#### 3.2.1 Entity 修改

`AdmissionQuota.java` 新增字段：

```java
@TableField("deadline")
private Instant deadline;
```

#### 3.2.2 QuotaController 变更

- `GET /api/v1/quotas` 返回数据中增加 `deadline` 字段
- `POST /api/v1/quotas`（创建/更新名额）时支持传入 `deadline`
- `PUT /api/v1/quotas/{quotaId}`（更新名额）时支持传入 `deadline`

#### 3.2.3 ScoreLineService 变更

现有接口 `GET /v1/score-lines` 已支持按学校查询，无需大改。

DTO `ScoreLineRequest` 已有 `majorId`、`year`、`subject`、`minScore` 四个字段：

- `subject='TOTAL'` 表示总分线
- 其他 subject 值表示对应科目的单科线

前端传入时：总分线的 `subject` 固定传 `'TOTAL'`，单科线传具体科目名（如 `'语文'`）。

### 3.3 前端 — QuotaManage.vue 扩展

在现有"名额管理"表格中新增两列：

#### 3.3.1 截止时间列

| 列 | 内容 |
|----|------|
| 录取截止时间 | 显示 `deadline` 值，无值显示 "-"；点击可快速编辑 |

#### 3.3.2 分数线列（总分 + 单科）

| 列 | 内容 |
|----|------|
| 分数线 | 显示"总分 X" + 各科分项；点击"配置分数线"跳转或弹窗 |

**点击"配置分数线" → 弹出分数线配置对话框**：

```
┌─────────────────────────────────────┐
│ 配置分数线 - 国际关系（博士） 2026  │
├─────────────────────────────────────┤
│ 【总分线】                         │
│ 总分最低分： [  300  ]             │
│                                     │
│ 【单科线】                         │
│ 科目        最低分                  │
│ 语文       [  80  ]  [删除]        │
│ 数学       [  75  ]  [删除]        │
│ [+ 添加单科]                        │
│                                     │
│        [取消]        [确定]         │
└─────────────────────────────────────┘
```

**数据结构设计**：

分数线数据通过现有 `/api/v1/score-lines` 接口管理：
- `GET /v1/score-lines` → 返回列表，按 `subject` 区分总分（`TOTAL`）和单科
- `POST /v1/score-lines` → 新增一条分数线（总分或单科）
- `PUT /v1/score-lines/{lineId}` → 更新单条
- `DELETE /v1/score-lines/{lineId}` → 删除单条

**总分线保存逻辑**：
- 保存时，遍历所有单科线，调用 POST/DELETE 接口同步
- 总分线：如果已存在则 PUT，不存在则 POST

---

## 4. 路由与菜单

### 院校侧菜单（SCHOOL_ADMIN / SCHOOL_STAFF）

```
配置管理
├── 专业配置       /majors
├── 名额管理       /quota          ← 扩展：截止时间 + 分数线入口
└── 招生简章       /brochure        ← 新增
```

---

## 5. 文件变更清单

| 文件 | 操作 |
|------|------|
| `db/migration/V17__add_deadline_and_total_score.sql` | 新建 |
| `entity/AdmissionQuota.java` | 修改 |
| `controller/SchoolBrochureController.java` | 新建 |
| `service/SchoolBrochureService.java` | 新建 |
| `controller/QuotaController.java` | 修改 |
| `views/school/SchoolBrochureConfig.vue` | 新建 |
| `views/school/QuotaManage.vue` | 修改 |
| `router/index.ts` | 修改 |
| `views/common/Layout.vue` | 修改 |

---

## 6. 实现顺序

1. **DB migration V17** — 新增 deadline 字段
2. **招生简章功能**（独立性强）— Controller + Service + 前端页面
3. **名额管理截止时间列** — Entity + Controller + 前端
4. **分数线配置** — 前端弹窗 + score-line 接口联调
