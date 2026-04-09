# 招生简章与名额管理扩展功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增招生简章编辑页面（富文本），并扩展名额管理页面支持录取截止时间和分数线（总分+单科）配置。

**Architecture:** 复用 `school_brochure` 表（无后端）新建 API；扩展 `admission_quota` 表新增 `deadline` 字段；分数线复用 `score_line` 表，subject='TOTAL' 代表总分线，其他为单科线。

**Tech Stack:** Spring Boot 3.4 / Vue 3 + Element Plus + Quill 富文本编辑器 / Flyway 迁移

---

## Step 0: 环境确认

**前置检查：** 后端运行在 `java -jar target/campus-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev`，前端运行在 `http://localhost:5178`

**测试账号：** school_admin2 / Aa123456!

---

## Phase 1: 数据库迁移

### Task 1: 创建 V17 迁移脚本

**文件：**
- 创建: `backend/src/main/resources/db/migration/V17__add_deadline_and_total_score.sql`

```sql
-- 新增录取截止时间字段
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS deadline TIMESTAMPTZ;

COMMENT ON COLUMN admission_quota.deadline IS '录取截止时间';
```

**验证：** 连接到数据库执行后，确认 `admission_quota` 表新增了 `deadline` 列。

---

## Phase 2: 招生简章功能

### Task 2: 新建 SchoolBrochure Entity

**文件：**
- 创建: `backend/src/main/java/com/campus/platform/entity/SchoolBrochure.java`

```java
package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school_brochure")
public class SchoolBrochure extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID brochureId;

    @TableField("school_id")
    private UUID schoolId;

    private String title;

    private String content;

    private String status; // DRAFT / PUBLISHED

    @TableField("published_at")
    private Instant publishedAt;
}
```

### Task 3: 新建 SchoolBrochureRepository

**文件：**
- 创建: `backend/src/main/java/com/campus/platform/repository/SchoolBrochureRepository.java`

```java
package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.SchoolBrochure;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

public interface SchoolBrochureRepository extends BaseMapper<SchoolBrochure> {

    @Select("SELECT * FROM school_brochure WHERE school_id = #{schoolId} AND deleted = 0 LIMIT 1")
    Optional<SchoolBrochure> findBySchoolId(@Param("schoolId") UUID schoolId);
}
```

### Task 4: 新建 SchoolBrochureService

**文件：**
- 创建: `backend/src/main/java/com/campus/platform/service/SchoolBrochureService.java`

```java
package com.campus.platform.service;

import com.campus.platform.entity.SchoolBrochure;
import com.campus.platform.repository.SchoolBrochureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolBrochureService {

    private final SchoolBrochureRepository brochureRepository;

    public SchoolBrochure getBySchool(UUID schoolId) {
        return brochureRepository.findBySchoolId(schoolId).orElse(null);
    }

    @Transactional
    public void save(UUID schoolId, String title, String content) {
        SchoolBrochure existing = brochureRepository.findBySchoolId(schoolId).orElse(null);
        if (existing != null) {
            existing.setTitle(title);
            existing.setContent(content);
            brochureRepository.updateById(existing);
            log.info("更新招生简章: schoolId={}", schoolId);
        } else {
            SchoolBrochure brochure = new SchoolBrochure();
            brochure.setBrochureId(UUID.randomUUID());
            brochure.setSchoolId(schoolId);
            brochure.setTitle(title);
            brochure.setContent(content);
            brochure.setStatus("DRAFT");
            brochureRepository.insert(brochure);
            log.info("新建招生简章: schoolId={}", schoolId);
        }
    }
}
```

### Task 5: 新建 SchoolBrochureController

**文件：**
- 创建: `backend/src/main/java/com/campus/platform/controller/SchoolBrochureController.java`

```java
package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.SchoolBrochureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "招生简章")
@RestController
@RequestMapping("/api/v1/brochures")
@RequiredArgsConstructor
public class SchoolBrochureController {

    private final SchoolBrochureService brochureService;

    @Operation(summary = "获取本校招生简章")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<SchoolBrochureVO> get() {
        UUID schoolId = SecurityContext.getSchoolId();
        SchoolBrochure b = brochureService.getBySchool(schoolId);
        if (b == null) {
            return Result.ok(SchoolBrochureVO.empty());
        }
        return Result.ok(new SchoolBrochureVO(b.getBrochureId().toString(), b.getTitle(), b.getContent()));
    }

    @Operation(summary = "保存招生简章")
    @PutMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> save(@RequestBody @Valid SchoolBrochureRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        brochureService.save(schoolId, req.getTitle(), req.getContent());
        return Result.ok();
    }

    @lombok.Data
    public static class SchoolBrochureRequest {
        private String title;
        private String content;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SchoolBrochureVO {
        private String brochureId;
        private String title;
        private String content;

        public static SchoolBrochureVO empty() {
            return new SchoolBrochureVO(null, "", "");
        }
    }
}
```

### Task 6: 安装 Quill 富文本编辑器

**文件：**
- 修改: `frontend/package.json` — 添加 `"@vueup/vue-quill": "^1.2.0"`

```bash
cd frontend && npm install @vueup/vue-quill
```

### Task 7: 新建 SchoolBrochureConfig.vue

**文件：**
- 创建: `frontend/src/views/school/SchoolBrochureConfig.vue`

页面结构：
- 顶部标题 "招生简章配置"
- 有数据时：显示标题 + 富文本内容预览（只读）+ 编辑按钮
- 无数据时：空状态提示 + 编辑按钮
- 点击编辑：切换为表单模式（标题输入 + Quill 富文本编辑器 + 保存/取消按钮）

Quill 编辑器使用 `@vueup/vue-quill` 的 `QuillEditor` 组件，主题用 `snow`。

```vue
<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">招生简章配置</h2>
    </div>
    <el-card>
      <!-- 只读预览模式 -->
      <div v-if="!editing">
        <el-alert v-if="!brochure.title" title="暂无招生简章" type="info" :closable="false" show-icon />
        <div v-else>
          <h3>{{ brochure.title }}</h3>
          <div v-html="brochure.content" class="content-preview"></div>
        </div>
        <el-button type="primary" style="margin-top: 16px" @click="startEdit">
          {{ brochure.title ? '编辑简章' : '创建简章' }}
        </el-button>
      </div>
      <!-- 编辑模式 -->
      <div v-else>
        <el-form :model="form" label-width="100px">
          <el-form-item label="简章标题">
            <el-input v-model="form.title" placeholder="请输入简章标题" maxlength="200" />
          </el-form-item>
          <el-form-item label="简章内容">
            <QuillEditor v-model:content="form.content" contentType="html" theme="snow" style="height: 400px" />
          </el-form-item>
        </el-form>
        <div style="margin-top: 16px; text-align: right">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const editing = ref(false)
const saving = ref(false)
const brochure = reactive({ brochureId: '', title: '', content: '' })
const form = reactive({ title: '', content: '' })

async function fetchBrochure() {
  const res = await axios.get('/v1/brochures')
  const data = res.data.data
  brochure.brochureId = data.brochureId || ''
  brochure.title = data.title || ''
  brochure.content = data.content || ''
}

function startEdit() {
  form.title = brochure.title
  form.content = brochure.content
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入简章标题')
    return
  }
  saving.value = true
  try {
    await axios.put('/v1/brochures', { title: form.title, content: form.content })
    Object.assign(brochure, form)
    editing.value = false
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(() => { fetchBrochure() })
</script>

<style scoped>
.content-preview { line-height: 1.8; color: #303133; }
:deep(.ql-editor) { height: 350px; }
</style>
```

### Task 8: 添加路由

**文件：**
- 修改: `frontend/src/router/index.ts` — 在院校路由部分添加

```ts
{
  path: '/brochure',
  name: 'SchoolBrochureConfig',
  component: () => import('@/views/school/SchoolBrochureConfig.vue'),
  meta: { title: '招生简章', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
},
```

### Task 9: 添加菜单入口

**文件：**
- 修改: `frontend/src/views/common/Layout.vue` — 在院校侧"配置管理"分组中添加

```vue
<el-menu-item index="/brochure">
  <el-icon><Document /></el-icon>
  <template #title>招生简章</template>
</el-menu-item>
```

在 import 中添加 `Document` 图标（如果尚未导入）。

---

## Phase 3: 截止时间功能

### Task 10: Entity 新增 deadline 字段

**文件：**
- 修改: `backend/src/main/java/com/campus/platform/entity/AdmissionQuota.java` — 添加字段

```java
@TableField("deadline")
private Instant deadline;
```

### Task 11: QuotaController GET 接口返回 deadline

**文件：**
- 修改: `backend/src/main/java/com/campus/platform/controller/QuotaController.java`

在 `list()` 方法的 map 中添加：
```java
m.put("deadline", q.getDeadline());
```

### Task 12: QuotaController POST/PUT 支持 deadline

**文件：**
- 修改: `backend/src/main/java/com/campus/platform/controller/QuotaController.java`

`QuotaRequest` DTO 添加：
```java
private Instant deadline;
```

`save()` 方法中设置 deadline：
```java
if (existing.isPresent()) {
    // ... existing.setTotalQuota 等
    quota.setDeadline(req.getDeadline());
    quotaRepository.updateById(quota);
} else {
    // ... new quota fields
    quota.setDeadline(req.getDeadline());
    quotaRepository.insert(quota);
}
```

`QuotaUpdateRequest` DTO 添加：
```java
private Instant deadline;
```

`update()` 方法中设置 deadline：
```java
quota.setDeadline(req.getDeadline());
```

### Task 13: 前端截止时间列

**文件：**
- 修改: `frontend/src/views/school/QuotaManage.vue`

在表格中添加截止时间列：
```html
<el-table-column label="录取截止时间" width="160">
  <template #default="{ row }">
    <span v-if="row.deadline">{{ formatDeadline(row.deadline) }}</span>
    <span v-else style="color: #909399">未设置</span>
  </template>
</el-table-column>
```

添加截止时间快捷编辑（点击弹出 datetime picker）：
```html
<template #default="{ row }">
  <el-popover placement="bottom" :width="280" trigger="click">
    <template #reference>
      <span class="deadline-cell">
        {{ row.deadline ? formatDeadline(row.deadline) : '点击设置' }}
      </span>
    </template>
    <div style="text-align: center">
      <el-date-picker
        v-model="deadlineDraft"
        type="datetime"
        placeholder="选择截止时间"
        style="width: 100%"
        format="YYYY-MM-DD HH:mm"
        @change="handleDeadlineChange(row.quotaId)"
      />
      <div style="margin-top: 8px">
        <el-button size="small" @click="clearDeadline(row.quotaId)">清除</el-button>
      </div>
    </div>
  </el-popover>
</template>
```

数据和方法：
```ts
const deadlineDraft = ref(null)

function formatDeadline(d: string): string {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function handleDeadlineChange(quotaId: string) {
  await axios.put(`/v1/quotas/${quotaId}`, { deadline: deadlineDraft.value ? deadlineDraft.value.toISOString() : null })
  fetchQuotas()
}

async function clearDeadline(quotaId: string) {
  await axios.put(`/v1/quotas/${quotaId}`, { deadline: null })
  fetchQuotas()
}
```

---

## Phase 4: 分数线配置

### Task 14: 前端 — 分数线配置弹窗

**文件：**
- 修改: `frontend/src/views/school/QuotaManage.vue`

在 `QuotaManage.vue` 中添加分数线配置对话框：

```html
<!-- 分数线配置弹窗 -->
<el-dialog v-model="scoreLineDialogVisible" :title="`配置分数线 - ${scoreLineMajorName} ${scoreLineYear}年`" width="600px">
  <!-- 总分线 -->
  <el-form-item label="总分最低分">
    <el-input-number v-model="scoreLineForm.totalScore" :min="0" :max="750" style="width: 100%" />
  </el-form-item>

  <!-- 单科线 -->
  <div style="margin: 16px 0; font-weight: 600">单科线</div>
  <el-table :data="scoreLineForm.subjects" stripe size="small">
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
  <el-button size="small" style="margin-top: 8px" @click="scoreLineForm.subjects.push({ subject: '', minScore: 0 })">
    + 添加单科
  </el-button>

  <template #footer>
    <el-button @click="scoreLineDialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="savingScoreLine" @click="saveScoreLines">确定</el-button>
  </template>
</el-dialog>
```

表格中分数线列：
```html
<el-table-column label="分数线" width="130">
  <template #default="{ row }">
    <el-button type="primary" link @click="openScoreLineDialog(row)">
      {{ row.hasScoreLine ? '查看' : '配置' }}
    </el-button>
  </template>
</el-table-column>
```

状态和方法：
```ts
const scoreLineDialogVisible = ref(false)
const savingScoreLine = ref(false)
const scoreLineMajorName = ref('')
const scoreLineYear = ref<number>()
const scoreLineQuotaId = ref('')
const scoreLineForm = reactive({
  totalScore: null as number | null,
  subjects: [] as { subject: string; minScore: number }[]
})

async function openScoreLineDialog(row: any) {
  scoreLineQuotaId.value = row.quotaId
  scoreLineMajorName.value = row.majorName
  scoreLineYear.value = row.year

  // 获取现有分数线
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

  scoreLineDialogVisible.value = true
}

async function saveScoreLines() {
  savingScoreLine.value = true
  try {
    // 获取当前页面专业的所有现有记录
    const res = await axios.get('/v1/score-lines')
    const existing = (res.data.data || []).filter((l: any) =>
      l.majorId === scoreLineMajorId.value && l.year === scoreLineYear.value
    )

    // 保存总分线
    const totalLine = existing.find((l: any) => l.subject === 'TOTAL')
    if (scoreLineForm.totalScore != null) {
      if (totalLine) {
        await axios.put(`/v1/score-lines/${totalLine.lineId}`, {
          majorId: scoreLineMajorId.value, year: scoreLineYear.value, subject: 'TOTAL', minScore: scoreLineForm.totalScore
        })
      } else {
        await axios.post('/v1/score-lines', {
          majorId: scoreLineMajorId.value, year: scoreLineYear.value, subject: 'TOTAL', minScore: scoreLineForm.totalScore
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
          majorId: scoreLineMajorId.value, year: scoreLineYear.value, subject: s.subject, minScore: s.minScore
        })
      }
    }

    scoreLineDialogVisible.value = false
    ElMessage.success('保存成功')
  } finally {
    savingScoreLine.value = false
  }
}
```

注意：`scoreLineMajorId` 需要在打开弹窗时从 row 中取到。

---

## Phase 5: 验证

### Task 15: 浏览器验证

使用 `school_admin2 / Aa123456!` 登录 `http://localhost:5178`，验证：

1. **招生简章页面** (`/brochure`)
   - 进入"配置管理 → 招生简章"，无数据时显示空状态
   - 点击"创建简章"，输入标题 + 富文本内容，保存后显示预览
   - 再次编辑，修改内容，保存验证

2. **名额管理页面** (`/quota`)
   - 查看表格是否新增"录取截止时间"列（无值显示"未设置"）
   - 点击"未设置"，弹出日期时间选择器，选择时间后确认
   - 验证截止时间在表格中正确显示
   - 点击"分数线"列的"配置"按钮，弹出分数线配置对话框
   - 填写总分 + 单科线（如语文80，数学75），保存
   - 再次点击"查看"，验证数据正确回显

3. **菜单验证**
   - 侧边栏"配置管理"分组下有"招生简章"菜单项

---

## 文件变更总览

| 阶段 | 文件 | 操作 |
|------|------|------|
| DB | `db/migration/V17__add_deadline_and_total_score.sql` | 新建 |
| Entity | `entity/AdmissionQuota.java` | 修改 |
| Entity | `entity/SchoolBrochure.java` | 新建 |
| Repository | `repository/SchoolBrochureRepository.java` | 新建 |
| Service | `service/SchoolBrochureService.java` | 新建 |
| Controller | `controller/SchoolBrochureController.java` | 新建 |
| Controller | `controller/QuotaController.java` | 修改 |
| Vue | `views/school/SchoolBrochureConfig.vue` | 新建 |
| Vue | `views/school/QuotaManage.vue` | 修改 |
| Router | `router/index.ts` | 修改 |
| Layout | `views/common/Layout.vue` | 修改 |
| Package | `frontend/package.json` | 修改（新增 vue-quill） |
