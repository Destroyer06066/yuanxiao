# 修复页面报错 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补全缺失的后端接口 + 修复空实现，消除登录后各页面的 404/500 报错。

**Architecture:** 纯后端 Controller 补全为主，少量前端 Dashboard 修复。不新建 Service 层（直接在 Controller 调用已有 Service/Repository），保持最小修改范围。

**Tech Stack:** Spring Boot 3 · MyBatis-Plus · Vue 3 · Element Plus

---

## 修复范围速览

| # | 问题 | 影响页面 | 类型 |
|---|---|---|---|
| 1 | `forceChangePassword` 空实现 | 强制改密 | 后端修复 |
| 2 | `confirmReset` 空实现 | 忘记密码 | 后端修复 |
| 3 | 缺 `POST /v1/admissions/final/{pushId}` | 考生详情-终裁 | 后端新增 |
| 4 | 缺 `POST /v1/admissions/revoke/{pushId}` | 考生详情-撤销 | 后端新增 |
| 5 | 缺 `/api/v1/supplement/rounds` 系列 | 补录管理 | 后端新增 |
| 6 | 缺 `/api/v1/checkins` 系列 | 报到管理 | 后端新增 |
| 7 | 缺 `/api/v1/score-lines` | 分数线配置 | 后端新增 |
| 8 | Dashboard 统计数据写死 | 工作台 | 前端修复 |

---

## Task 1: 修复 AccountRepository + AuthController 密码接口

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/repository/AccountRepository.java`
- Modify: `backend/src/main/java/com/campus/platform/controller/AuthController.java`

### Step 1: 在 AccountRepository 追加 updatePassword 方法

在 `AccountRepository` 接口末尾追加：

```java
@Update("UPDATE account SET password_hash = #{passwordHash}, must_change_password = false " +
        "WHERE account_id = #{accountId}")
int updatePassword(@Param("accountId") UUID accountId,
                   @Param("passwordHash") String passwordHash);

default Optional<Account> findByUsernameDirect(String username) {
    LambdaQueryWrapper<Account> q = new LambdaQueryWrapper<>();
    q.apply("LOWER(username) = LOWER({0})", username).last("LIMIT 1");
    return Optional.ofNullable(selectOne(q));
}
```

注意：`findByUsernameDirect` 已存在类似方法（`findByUsername`），这里直接复用 `findByUsername` 即可，不需要新增。

### Step 2: 修复 AuthController.forceChangePassword

替换当前空实现：

```java
@Operation(summary = "强制修改密码（首次登录）")
@PutMapping("/password/force-change")
public Result<Void> forceChangePassword(@RequestBody @Valid PasswordChangeRequest req) {
    AccountPrincipal current = SecurityContext.get();
    if (current == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
    }
    Account account = accountRepository.findById(current.getAccountId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在"));
    // 新密码不能与旧密码相同
    if (passwordEncoder.matches(req.getNewPassword(), account.getPasswordHash())) {
        throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD, "新密码不能与旧密码相同");
    }
    String newHash = passwordEncoder.encode(req.getNewPassword());
    accountRepository.updatePassword(current.getAccountId(), newHash);
    return Result.ok();
}
```

同时在 AuthController 类顶部注入 AccountRepository（已有 PasswordEncoder 注入，追加一行）：

```java
private final AccountRepository accountRepository;
```

### Step 3: 修复 AuthController.confirmReset

替换注释掉的密码更新逻辑：

```java
@Operation(summary = "验证并重置密码")
@PostMapping("/password/reset/confirm")
public Result<Void> confirmReset(@RequestBody @Valid ResetConfirmRequest req) {
    String stored = redisService.getSmsCode(req.getUsername());
    if (stored == null) {
        throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED, "验证码已过期");
    }
    if (!stored.equals(req.getCode())) {
        throw new BusinessException(ErrorCode.SMS_CODE_ERROR, "验证码错误");
    }
    redisService.deleteSmsCode(req.getUsername());
    // 更新密码
    Account account = accountRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在"));
    String newHash = passwordEncoder.encode(req.getNewPassword());
    accountRepository.updatePassword(UUID.fromString(account.getAccountId()), newHash);
    return Result.ok();
}
```

### Step 4: 编译验证

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -10
```

期望：`BUILD SUCCESS`，无编译错误。

### Step 5: Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add backend/src/main/java/com/campus/platform/repository/AccountRepository.java \
        backend/src/main/java/com/campus/platform/controller/AuthController.java
git commit -m "fix: implement forceChangePassword and confirmReset in AuthController"
```

---

## Task 2: 新增终裁录取 / 撤销录取接口

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/controller/StudentController.java`

`student.ts` 调用的路径是 `/v1/admissions/final/{pushId}` 和 `/v1/admissions/revoke/{pushId}`（注意无 `/api` 前缀，axios baseURL 已加）。

AdmissionService 已有 `finalAdmission(UUID pushId, UUID operatorId)` 和 `revokeAdmission(UUID pushId, UUID operatorId)`，只需新增 Controller 方法。

### Step 1: 新建 AdmissionController

创建文件 `backend/src/main/java/com/campus/platform/controller/AdmissionController.java`：

```java
package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "录取操作")
@RestController
@RequestMapping("/api/v1/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @Operation(summary = "终裁录取（有条件→正式录取）")
    @PostMapping("/final/{pushId}")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> finalAdmission(@PathVariable UUID pushId) {
        admissionService.finalAdmission(pushId, SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "撤销录取")
    @PostMapping("/revoke/{pushId}")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> revokeAdmission(@PathVariable UUID pushId) {
        admissionService.revokeAdmission(pushId, SecurityContext.getAccountId());
        return Result.ok();
    }
}
```

### Step 2: 编译验证

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -5
```

### Step 3: Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add backend/src/main/java/com/campus/platform/controller/AdmissionController.java
git commit -m "feat: add finalAdmission and revokeAdmission endpoints"
```

---

## Task 3: 补录轮次管理接口

**Files:**
- Create: `backend/src/main/java/com/campus/platform/controller/SupplementController.java`

前端调用的 API：
- `GET /api/v1/supplement/rounds` — 获取所有轮次
- `POST /api/v1/supplement/rounds` — 创建轮次（body: roundNumber, startTime, endTime, remark）
- `PATCH /api/v1/supplement/rounds/{roundId}` — 修改状态（body: status）

SupplementRound 实体和 SupplementRoundRepository 已存在。

### Step 1: 创建 SupplementController

```java
package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.SupplementRound;
import com.campus.platform.repository.SupplementRoundRepository;
import com.campus.platform.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "补录管理")
@RestController
@RequestMapping("/api/v1/supplement/rounds")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementRoundRepository supplementRoundRepository;

    @Operation(summary = "获取所有补录轮次")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<SupplementRound>> list() {
        return Result.ok(supplementRoundRepository.findAll());
    }

    @Operation(summary = "创建补录轮次")
    @PostMapping
    @RequireRole({"OP_ADMIN"})
    public Result<SupplementRound> create(@RequestBody @Valid CreateRoundRequest req) {
        SupplementRound round = new SupplementRound();
        round.setRoundId(UUID.randomUUID());
        round.setRoundNumber(req.getRoundNumber());
        round.setStartTime(Instant.parse(req.getStartTime().replace(" ", "T") + (req.getStartTime().contains("T") ? "" : ":00Z").replace("Z", "+08:00").replace("+08:00+08:00", "+08:00")));
        round.setEndTime(Instant.parse(req.getEndTime().replace(" ", "T") + (req.getEndTime().contains("T") ? "" : ":00Z").replace("Z", "+08:00").replace("+08:00+08:00", "+08:00")));
        round.setRemark(req.getRemark());
        round.setStatus("UPCOMING");
        supplementRoundRepository.insert(round);
        return Result.ok(round);
    }

    @Operation(summary = "修改补录轮次状态")
    @PatchMapping("/{roundId}")
    @RequireRole({"OP_ADMIN"})
    public Result<Void> updateStatus(@PathVariable UUID roundId,
                                     @RequestBody UpdateStatusRequest req) {
        SupplementRound round = supplementRoundRepository.findById(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND, "补录轮次不存在"));
        round.setStatus(req.getStatus());
        supplementRoundRepository.updateById(round);
        return Result.ok();
    }

    @Data
    public static class CreateRoundRequest {
        private Integer roundNumber;
        @NotBlank private String startTime;
        @NotBlank private String endTime;
        private String remark;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank private String status;
    }
}
```

**注意：** 时间字符串解析逻辑较复杂（前端传 `"2026-03-27 10:00:00"` 格式），用更简洁的 LocalDateTime 解析替代：

**实际创建时使用此版本（替换 create 方法中时间解析部分）：**

```java
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 在 create 方法中：
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
Instant startInstant = LocalDateTime.parse(req.getStartTime(), fmt)
        .atZone(ZoneId.of("Asia/Shanghai")).toInstant();
Instant endInstant = LocalDateTime.parse(req.getEndTime(), fmt)
        .atZone(ZoneId.of("Asia/Shanghai")).toInstant();
round.setStartTime(startInstant);
round.setEndTime(endInstant);
```

### Step 2: 编译验证

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -5
```

### Step 3: Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add backend/src/main/java/com/campus/platform/controller/SupplementController.java
git commit -m "feat: add SupplementController for supplement round management"
```

---

## Task 4: 报到管理接口

**Files:**
- Create: `backend/src/main/java/com/campus/platform/controller/CheckinController.java`

前端调用：
- `GET /api/v1/checkins` — 获取该校所有 CONFIRMED/MATERIAL_RECEIVED/CHECKED_IN 状态考生
- `POST /api/v1/material-receive` — body: `{pushId}` → 状态 CONFIRMED → MATERIAL_RECEIVED
- `POST /api/v1/checkin` — body: `{pushId}` → 状态 MATERIAL_RECEIVED → CHECKED_IN

前端期望字段：`candidateName`, `majorName`, `statusDesc`, `receiveTime`, `checkinTime`, `status`, `pushId`

CandidatePush 实体无 `receiveTime`/`checkinTime` 字段，用 `operatedAt` 代替，`majorName` 从 MajorRepository 获取。

### Step 1: 创建 CheckinController

```java
package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "报到管理")
@RestController
@RequiredArgsConstructor
public class CheckinController {

    private final CandidatePushRepository candidatePushRepository;
    private final MajorRepository majorRepository;

    @Operation(summary = "获取报到列表")
    @GetMapping("/api/v1/checkins")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        UUID schoolId = SecurityContext.getSchoolId();
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        q.eq(CandidatePush::getSchoolId, schoolId)
         .in(CandidatePush::getStatus, List.of(
                 CandidateStatus.CONFIRMED.name(),
                 CandidateStatus.MATERIAL_RECEIVED.name(),
                 CandidateStatus.CHECKED_IN.name()))
         .orderByDesc(CandidatePush::getOperatedAt);

        List<CandidatePush> pushes = candidatePushRepository.selectList(q);

        List<Map<String, Object>> result = pushes.stream().map(p -> {
            String majorName = p.getAdmissionMajorId() != null
                    ? majorRepository.findById(p.getAdmissionMajorId())
                            .map(m -> m.getMajorName()).orElse("")
                    : "";
            String statusDesc = CandidateStatus.valueOf(p.getStatus()).getDescription();
            return Map.<String, Object>of(
                    "pushId", p.getPushId().toString(),
                    "candidateName", p.getCandidateName(),
                    "majorName", majorName,
                    "status", p.getStatus(),
                    "statusDesc", statusDesc,
                    "receiveTime", p.getStatus().equals(CandidateStatus.MATERIAL_RECEIVED.name()) || p.getStatus().equals(CandidateStatus.CHECKED_IN.name())
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : "",
                    "checkinTime", p.getStatus().equals(CandidateStatus.CHECKED_IN.name())
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : ""
            );
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @Operation(summary = "登记收件")
    @PostMapping("/api/v1/material-receive")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> receiveMaterial(@RequestBody PushIdRequest req) {
        CandidatePush push = candidatePushRepository.findById(UUID.fromString(req.getPushId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));
        if (!CandidateStatus.CONFIRMED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许登记收件");
        }
        push.setStatus(CandidateStatus.MATERIAL_RECEIVED.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(SecurityContext.getAccountId());
        candidatePushRepository.updateById(push);
        return Result.ok();
    }

    @Operation(summary = "确认报到")
    @PostMapping("/api/v1/checkin")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> checkin(@RequestBody PushIdRequest req) {
        CandidatePush push = candidatePushRepository.findById(UUID.fromString(req.getPushId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));
        if (!CandidateStatus.MATERIAL_RECEIVED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "请先登记收件");
        }
        push.setStatus(CandidateStatus.CHECKED_IN.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(SecurityContext.getAccountId());
        candidatePushRepository.updateById(push);
        return Result.ok();
    }

    @Data
    public static class PushIdRequest {
        private String pushId;
    }
}
```

### Step 2: 编译验证

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -5
```

### Step 3: Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add backend/src/main/java/com/campus/platform/controller/CheckinController.java
git commit -m "feat: add CheckinController for material receive and checkin"
```

---

## Task 5: 分数线接口（返回空列表，消除 404）

**Files:**
- Create: `backend/src/main/java/com/campus/platform/controller/ScoreLineController.java`

分数线数据库表/实体未实现，仅需返回空列表防止前端报错。

### Step 1: 创建 ScoreLineController

```java
package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分数线配置")
@RestController
@RequestMapping("/api/v1/score-lines")
public class ScoreLineController {

    @Operation(summary = "获取分数线列表（暂未实现）")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Object>> list() {
        return Result.ok(List.of());
    }
}
```

### Step 2: 编译 + Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -5
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add backend/src/main/java/com/campus/platform/controller/ScoreLineController.java
git commit -m "feat: add ScoreLineController stub to prevent 404"
```

---

## Task 6: 修复 Dashboard 数据加载

**Files:**
- Modify: `frontend/src/views/common/Dashboard.vue`

当前 `stats` 是写死数据，`recentOps` 永远为空。从 Statistics API 加载真实数据。

### Step 1: 修改 Dashboard.vue 的 `<script setup>` 部分

替换现有 script 为：

```typescript
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

const authStore = useAuthStore()

const stats = ref([
  { label: '待处理考生', value: 0, icon: 'Clock', color: '#409eff' },
  { label: '已录取', value: 0, icon: 'Trophy', color: '#67c23a' },
  { label: '已确认', value: 0, icon: 'Check', color: '#e6a23c' },
  { label: '已报到', value: 0, icon: 'User', color: '#25a861' },
])

const recentOps = ref<any[]>([])

async function loadStats() {
  try {
    const res = await axios.get('/api/v1/statistics/kpis')
    const d = res.data.data || {}
    stats.value[0].value = d.totalPushed ?? 0
    stats.value[1].value = d.admitted ?? 0
    stats.value[2].value = d.confirmed ?? 0
    stats.value[3].value = d.checkedIn ?? 0
  } catch (e) {
    // 统计接口失败不影响页面渲染
  }
}

onMounted(() => {
  authStore.fetchUserInfo()
  loadStats()
})
</script>
```

### Step 2: Commit

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git add frontend/src/views/common/Dashboard.vue
git commit -m "fix: load real stats in Dashboard from statistics API"
```

---

## Task 7: 全量编译验证 + Push

### Step 1: 后端全量编译

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform/backend"
mvn compile -q 2>&1 | tail -10
```

期望：`BUILD SUCCESS`

### Step 2: 运行所有单元测试

```bash
mvn test -q 2>&1 | tail -10
```

期望：66 tests, 0 failures

### Step 3: Push 到 GitHub

```bash
cd "/Volumes/HP P900/Tech/yuanxiao/campus-platform"
git push origin main
```

---

## 修复后预期效果

| 页面 | 修复前 | 修复后 |
|---|---|---|
| 强制改密 | 提交后密码未变 | 密码正确更新 |
| 忘记密码 | 确认后密码未变 | 密码正确更新 |
| 考生详情-终裁 | 404 报错 | 正常调用 |
| 考生详情-撤销 | 404 报错 | 正常调用 |
| 补录管理 | 404 报错，页面空白 | 显示轮次列表 |
| 报到管理 | 404 报错，页面空白 | 显示报到列表 |
| 分数线配置 | 404 报错 | 显示空列表（正常） |
| 工作台 | 统计数据写死 | 从 API 读取真实数据 |
