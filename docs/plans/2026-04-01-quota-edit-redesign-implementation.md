# 名额管理编辑弹窗重构实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将截止时间、开始时间、分数线全部收敛到"编辑名额"一个弹窗中。

**Architecture:** 后端 Entity 新增 startTime 字段，DTD 新增 startTime，Controller 映射之。前端重构编辑弹窗，移除表格独立列和独立分数线弹窗。

**Tech Stack:** Spring Boot 3.4 (MyBatis-Plus) / Vue 3 + Element Plus + TypeScript

---

## Task 1: DB migration V18 — 新增 start_time 字段

**Files:**
- Create: `backend/src/main/resources/db/migration/V18__add_start_time.sql`

**Step 1: Write migration**

```sql
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ;
COMMENT ON COLUMN admission_quota.start_time IS '录取开始时间';
```

**Step 2: Run migration**

```bash
psql -h localhost -U postgres -d campus_platform -c "ALTER TABLE admission_quota ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ;"
```

**Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V18__add_start_time.sql
git commit -m "feat(db): V18 add start_time to admission_quota"
```

---

## Task 2: 后端 Entity 新增 startTime 字段

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/entity/AdmissionQuota.java`

**Step 1: 新增字段**

在 `deadline` 字段旁新增：

```java
@TableField("start_time")
private Instant startTime;
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/campus/platform/entity/AdmissionQuota.java
git commit -m "feat(entity): add startTime field to AdmissionQuota"
```

---

## Task 3: DTO 新增 startTime

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/controller/QuotaController.java`

**Step 1: QuotaRequest 新增 startTime**

```java
@Data
public static class QuotaRequest {
    @NotNull private UUID majorId;
    @Min(0) private Integer totalQuota;
    private Integer year;
    private Integer minScore;
    private Integer maxScore;
    private Instant startTime;   // 新增
    private Instant deadline;
}
```

**Step 2: QuotaUpdateRequest 新增 startTime**

```java
@Data
public static class QuotaUpdateRequest {
    @Min(0) @NotNull private Integer totalQuota;
    private Integer minScore;
    private Integer maxScore;
    private Instant startTime;   // 新增
    private Instant deadline;
}
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/campus/platform/controller/QuotaController.java
git commit -m "feat(dto): add startTime to QuotaRequest and QuotaUpdateRequest"
```

---

## Task 4: Controller save/update 方法支持 startTime

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/controller/QuotaController.java`

**Step 1: GET list 返回 startTime**

在返回的 HashMap 中加入 `startTime` 字段（`put("startTime", q.getStartTime())`）。

**Step 2: save() 方法**

在创建时 `quota.setDeadline(req.getDeadline())` 后新增：
```java
quota.setStartTime(req.getStartTime());
```

**Step 3: update() 方法**

在更新设置字段后新增：
```java
quota.setStartTime(req.getStartTime());
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/campus/platform/controller/QuotaController.java
git commit -m "feat(api): support startTime in quota save/update"
```

---

## Task 5: 前端重构 QuotaManage.vue — 移除表格列

**Files:**
- Modify: `frontend/src/views/school/QuotaManage.vue`

**Step 1: 移除"录取截止时间"列**

删除第 54-77 行的 `el-table-column`（截止时间 popover）。

**Step 2: 移除"分数线"列**

删除第 78-82 行的 `el-table-column`（配置按钮）。

**Step 3: 移除 deadlineDraft ref**

删除第 243 行 `const deadlineDraft = ref(null)`。

**Step 4: 移除 formatDeadline/handleDeadlineChange/clearDeadline 函数**

删除第 245-263 行相关函数。

**Step 5: 移除 deadline-cell 样式**

删除 `.deadline-cell:hover { text-decoration: underline; }` 行。

**Step 6: Commit**

```bash
git add frontend/src/views/school/QuotaManage.vue
git commit -m "refactor(frontend): remove deadline and score-line table columns"
```

---

## Task 6: 前端重构编辑弹窗 — 新增 startTime/deadline/分数线

**Files:**
- Modify: `frontend/src/views/school/QuotaManage.vue`

**Step 1: 修改弹窗宽度**

第 107 行：`width="520px"` → `width="680px"`

**Step 2: form 新增字段**

第 228-234 行 form 对象改为：

```typescript
const form = reactive({
  majorId: '',
  year: currentYear,
  totalQuota: 0,
  minScore: null as number | null,
  maxScore: null as number | null,
  startTime: null as Date | null,    // 新增
  deadline: null as Date | null,      // 新增
})
```

**Step 3: 弹窗内字段布局（编辑模式）**

第 112-144 行替换为：

```vue
<el-form-item label="专业" prop="majorId">
  <el-select v-model="form.majorId" placeholder="请选择专业" style="width: 100%" filterable :disabled="isEdit">
    <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
  </el-select>
</el-form-item>
<el-form-item label="年份" prop="year">
  <el-select v-model="form.year" placeholder="请选择年份" style="width: 100%" :disabled="isEdit">
    <el-option :label="currentYear" :value="currentYear" />
    <el-option :label="nextYear" :value="nextYear" />
  </el-select>
</el-form-item>
<el-form-item label="录取时间段">
  <div style="display: flex; gap: 8px; align-items: center; width: 100%">
    <el-date-picker
      v-model="form.startTime"
      type="datetime"
      placeholder="开始时间（可空）"
      format="YYYY-MM-DD HH:mm"
      style="width: 50%"
    />
    <span style="color: #909399">~</span>
    <el-date-picker
      v-model="form.deadline"
      type="datetime"
      placeholder="截止时间"
      format="YYYY-MM-DD HH:mm"
      style="width: 50%"
    />
  </div>
</el-form-item>
<el-form-item label="总名额" prop="totalQuota">
  <el-input-number v-model="form.totalQuota" :min="0" :max="99999" placeholder="请输入总名额数" style="width: 100%" />
</el-form-item>
<el-form-item label="录取分数区间">
  <div style="display: flex; gap: 8px; align-items: center; width: 100%">
    <el-input-number v-model="form.minScore" :min="0" :max="750" placeholder="最低分" style="width: 50%" />
    <span style="color: #909399">~</span>
    <el-input-number v-model="form.maxScore" :min="0" :max="750" placeholder="最高分" style="width: 50%" />
  </div>
</el-form-item>
<hr style="border: none; border-top: 1px solid #eee; margin: 16px 0" />
<div style="font-weight: 600; margin-bottom: 12px">分数线配置</div>
<el-form-item label="总分最低分">
  <el-input-number v-model="scoreLineForm.totalScore" :min="0" :max="750" style="width: 100%" />
</el-form-item>
<div style="margin-bottom: 8px; color: #606266">单科线</div>
<el-table :data="scoreLineForm.subjects" stripe size="small" style="margin-bottom: 8px">
  <el-table-column prop="subject" label="科目" width="200">
    <template #default="{ row }">
      <el-input v-model="row.subject" placeholder="如：语文" />
    </template>
  </el-table-column>
  <el-table-column prop="minScore" label="最低分" min-width="150">
    <template #default="{ row }">
      <el-input-number v-model="row.minScore" :min="0" :max="200" style="width: 100%" />
    </template>
  </el-table-column>
  <el-table-column width="80">
    <template #default="{ $index }">
      <el-button type="danger" link @click="scoreLineForm.subjects.splice($index, 1)">删除</el-button>
    </template>
  </el-table-column>
</el-table>
<el-button size="small" @click="scoreLineForm.subjects.push({ subject: '', minScore: 0 })">+ 添加单科</el-button>
<!-- 编辑时显示已占用信息 -->
<el-alert
  v-if="isEdit && editingRow"
  type="warning"
  :closable="false"
  show-icon
  style="margin-top: 16px"
>
  当前已录取 <strong>{{ editingRow.enrolledCount }}</strong> 人，已预占 <strong>{{ editingRow.reservedCount }}</strong> 人，
  剩余可用名额 <strong>{{ (editingRow.totalQuota || 0) - (editingRow.enrolledCount || 0) - (editingRow.reservedCount || 0) }}</strong> 个。
  新总名额不得少于 <strong>{{ (editingRow.enrolledCount || 0) + (editingRow.reservedCount || 0) }}</strong>。
</el-alert>
```

**Step 4: 新增 scoreLineForm 移入编辑弹窗**

将第 401-411 行的独立 `scoreLineDialogVisible`/`scoreLineForm` 等 ref/reactive 移除（因为不再有独立弹窗），scoreLineForm 改为在弹窗表单内直接使用。

**Step 5: resetForm 补充新字段**

```typescript
function resetForm() {
  formRef.value?.resetFields()
  form.majorId = ''
  form.year = currentYear
  form.totalQuota = 0
  form.minScore = null
  form.maxScore = null
  form.startTime = null    // 新增
  form.deadline = null     // 新增
  editingRow.value = null
  scoreLineForm.totalScore = null
  scoreLineForm.subjects = []
}
```

**Step 6: openEdit 补充 startTime/deadline 和分数线加载**

```typescript
async function openEdit(row: any) {
  isEdit.value = true
  editingId.value = row.quotaId
  editingRow.value = row
  Object.assign(form, {
    majorId: row.majorId,
    year: row.year,
    totalQuota: row.totalQuota,
    minScore: row.minScore ?? null,
    maxScore: row.maxScore ?? null,
    startTime: row.startTime ? new Date(row.startTime) : null,
    deadline: row.deadline ? new Date(row.deadline) : null,
  })
  // 加载分数线
  const res = await axios.get('/v1/score-lines')
  const lines = (res.data.data || []).filter((l: any) => l.majorId === row.majorId && l.year === row.year)
  scoreLineForm.totalScore = null
  scoreLineForm.subjects = []
  for (const line of lines) {
    if (line.subject === 'TOTAL') {
      scoreLineForm.totalScore = line.minScore
    } else {
      scoreLineForm.subjects.push({ subject: line.subject, minScore: line.minScore })
    }
  }
  dialogVisible.value = true
}
```

**Step 7: submitForm 补充 startTime/deadline/scoreLine 保存逻辑**

```typescript
async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (isEdit.value && editingRow.value) {
    const minRequired = (editingRow.value.enrolledCount || 0) + (editingRow.value.reservedCount || 0)
    if (form.totalQuota < minRequired) {
      ElMessage.error(`总名额不得少于已占用名额（${minRequired}），请先调整录取状态`)
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/v1/quotas/${editingId.value}`, {
        totalQuota: form.totalQuota,
        minScore: form.minScore,
        maxScore: form.maxScore,
        startTime: form.startTime ? new Date(form.startTime).toISOString() : null,
        deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      })
      // 保存分数线
      await saveScoreLines()
    } else {
      await axios.post('/v1/quotas', {
        majorId: form.majorId,
        year: form.year,
        totalQuota: form.totalQuota,
        minScore: form.minScore,
        maxScore: form.maxScore,
        startTime: form.startTime ? new Date(form.startTime).toISOString() : null,
        deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      })
      // 新建模式下也保存分数线（新建quota后再保存）
      await saveScoreLines()
    }
    dialogVisible.value = false
    fetchQuotas()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}
```

**Step 8: saveScoreLines 改为内部调用（移除独立弹窗逻辑）**

`scoreLineMajorId` 和 `scoreLineYear` 改为从 `form` 或 `editingRow` 获取：

```typescript
async function saveScoreLines() {
  const majorId = form.majorId
  const year = form.year
  // 获取当前专业的所有现有记录
  const res = await axios.get('/v1/score-lines')
  const existing = (res.data.data || []).filter((l: any) => l.majorId === majorId && l.year === year)

  // 保存总分线
  const totalLine = existing.find((l: any) => l.subject === 'TOTAL')
  if (scoreLineForm.totalScore != null) {
    if (totalLine) {
      await axios.put(`/v1/score-lines/${totalLine.lineId}`, {
        majorId, year, subject: 'TOTAL', minScore: scoreLineForm.totalScore
      })
    } else {
      await axios.post('/v1/score-lines', {
        majorId, year, subject: 'TOTAL', minScore: scoreLineForm.totalScore
      })
    }
  } else if (totalLine) {
    await axios.delete(`/v1/score-lines/${totalLine.lineId}`)
  }

  // 保存单科线（先删后增）
  const subjectLines = existing.filter((l: any) => l.subject !== 'TOTAL')
  for (const sl of subjectLines) {
    await axios.delete(`/v1/score-lines/${sl.lineId}`)
  }
  for (const s of scoreLineForm.subjects) {
    if (s.subject && s.minScore != null) {
      await axios.post('/v1/score-lines', {
        majorId, year, subject: s.subject, minScore: s.minScore
      })
    }
  }
  ElMessage.success('保存成功')
}
```

**Step 9: 移除 scoreLineDialogVisible/scoreLineDialog 和 openScoreLineDialog 函数**

删除第 152-191 行的独立分数线弹窗，以及 `openScoreLineDialog` 函数。

**Step 10: Commit**

```bash
git add frontend/src/views/school/QuotaManage.vue
git commit -m "refactor(frontend): consolidate deadline, startTime, score lines into edit dialog"
```

---

## Task 7: 浏览器测试

**Step 1: 重启后端**

```bash
pkill -f "campus-platform.*jar" || true
cd backend && mvn clean package -DskipTests -q
nohup java -jar target/campus-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev > /tmp/backend.log 2>&1 &
sleep 8
```

**Step 2: 用 school_admin2 登录，点击"名额管理"**

- 验证表格中**没有**"录取截止时间"列和"分数线"列
- 点击"编辑"，验证弹窗宽度约 680px
- 验证弹窗中有：专业（只读）+ 年份（只读）+ 录取时间段（两个date-picker）+ 总名额 + 录取分数区间 + 分数线配置（总分 + 单科线）
- 设置开始时间和截止时间，保存后刷新页面验证显示正确
- 新建名额，验证也可以填写时间段和分数线
