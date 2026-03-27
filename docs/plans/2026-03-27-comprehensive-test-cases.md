# 院校管理平台 测试用例补全计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 对现有 4 个 Service 单元测试文件进行覆盖率分析，新增 2 个测试文件，合计补充约 35 个测试用例，覆盖所有核心业务分支。

**Architecture:** 全部为 Service 层单元测试，使用 Mockito 隔离依赖（Repository、RedisService、外部客户端），不依赖数据库或 Spring 容器。新增测试追加到现有 Nested 类末尾，或创建新的测试文件。

**Tech Stack:** JUnit 5 · Mockito 5 · Spring Boot Test · JJWT (jjwt-api)

---

## 覆盖率现状分析

| 测试文件 | 现有用例数 | 主要缺口 |
|---|---|---|
| AccountServiceTest | 9 | createSchoolAdmin、createStaff 完全未覆盖 |
| AdmissionServiceTest | 11 | conditionalAdmission/finalAdmission/revoke 缺边界场景 |
| CandidateServiceTest | 8 | receivePush PENDING/CONDITIONAL 重推场景缺失 |
| StatisticsServiceTest | 5 | 基本覆盖，无需补充 |
| JwtTokenProviderTest | **0** | 全新文件，generateToken/parseToken/validateToken |
| ConditionalExpiryTaskTest | **0** | 全新文件，定时任务批量处理逻辑 |

---

## Task 1: AccountService — createSchoolAdmin 测试

**Files:**
- Modify: `backend/src/test/java/com/campus/platform/service/AccountServiceTest.java`

在现有 `LogoutTests` 类之后，追加以下 `@Nested` 块：

**Step 1: 追加 CreateSchoolAdminTests 到 AccountServiceTest**

```java
// ========== 创建院校管理员测试 ==========

@Nested
@DisplayName("createSchoolAdmin")
class CreateSchoolAdminTests {

    private UUID schoolId;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        operatorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("成功创建院校管理员 → 返回初始密码，写入 Redis")
    void createSchoolAdmin_success() {
        when(accountRepository.existsSchoolAdmin(schoolId)).thenReturn(false);
        when(accountRepository.existsByUsername("blcu_admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        doAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setAccountId(UUID.randomUUID().toString());
            return 1;
        }).when(accountRepository).insert(any(Account.class));
        doNothing().when(redisService).set(anyString(), anyString(), any());

        String password = accountService.createSchoolAdmin(schoolId, "blcu_admin",
                "王招生", "13800138000", operatorId);

        assertNotNull(password);
        assertTrue(password.length() >= 12);
        verify(accountRepository).insert(argThat(a ->
                AccountRole.SCHOOL_ADMIN.name().equals(a.getRole()) &&
                schoolId.equals(a.getSchoolId()) &&
                Boolean.TRUE.equals(a.getMustChangePassword())
        ));
        verify(redisService).set(
                argThat(k -> k.startsWith("init_pwd:")),
                eq(password),
                any()
        );
    }

    @Test
    @DisplayName("该校已有管理员 → 抛出 SCHOOL_ADMIN_EXISTS")
    void createSchoolAdmin_alreadyExists() {
        when(accountRepository.existsSchoolAdmin(schoolId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createSchoolAdmin(schoolId, "blcu_admin",
                        "王招生", "13800138000", operatorId));

        assertEquals(ErrorCode.SCHOOL_ADMIN_EXISTS, ex.getCode());
        verify(accountRepository, never()).insert(any());
    }

    @Test
    @DisplayName("用户名已存在 → 抛出 USERNAME_ALREADY_EXISTS")
    void createSchoolAdmin_usernameTaken() {
        when(accountRepository.existsSchoolAdmin(schoolId)).thenReturn(false);
        when(accountRepository.existsByUsername("blcu_admin")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createSchoolAdmin(schoolId, "blcu_admin",
                        "王招生", "13800138000", operatorId));

        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, ex.getCode());
        verify(accountRepository, never()).insert(any());
    }
}
```

**Step 2: 运行测试验证通过**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/backend
mvn test -Dtest=AccountServiceTest -pl . -q
```

期望输出: `BUILD SUCCESS`，3 个新测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/service/AccountServiceTest.java
git commit -m "test: add createSchoolAdmin tests to AccountServiceTest"
```

---

## Task 2: AccountService — createStaff 测试

**Files:**
- Modify: `backend/src/test/java/com/campus/platform/service/AccountServiceTest.java`

在 `CreateSchoolAdminTests` 之后追加：

**Step 1: 追加 CreateStaffTests**

```java
// ========== 创建工作人员测试 ==========

@Nested
@DisplayName("createStaff")
class CreateStaffTests {

    private UUID schoolId;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        operatorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("未传初始密码 → 系统生成，成功创建工作人员")
    void createStaff_success_generatedPassword() {
        when(accountRepository.existsByUsername("staff01")).thenReturn(false);
        when(accountRepository.countActiveBySchool(schoolId)).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        doAnswer(invocation -> 1).when(accountRepository).insert(any(Account.class));

        String password = accountService.createStaff(schoolId, "staff01",
                "李老师", "13900139000", null, operatorId);

        assertNotNull(password);
        assertTrue(password.length() >= 12);
        verify(accountRepository).insert(argThat(a ->
                AccountRole.SCHOOL_STAFF.name().equals(a.getRole()) &&
                Boolean.TRUE.equals(a.getMustChangePassword())
        ));
    }

    @Test
    @DisplayName("传入自定义初始密码 → 使用用户指定密码")
    void createStaff_success_customPassword() {
        when(accountRepository.existsByUsername("staff02")).thenReturn(false);
        when(accountRepository.countActiveBySchool(schoolId)).thenReturn(0);
        when(passwordEncoder.encode("MyPass123")).thenReturn("$2a$10$hashed");
        doAnswer(invocation -> 1).when(accountRepository).insert(any(Account.class));

        String password = accountService.createStaff(schoolId, "staff02",
                "张老师", null, "MyPass123", operatorId);

        assertEquals("MyPass123", password);
        verify(passwordEncoder).encode("MyPass123");
    }

    @Test
    @DisplayName("工作人员数量达上限(50) → 抛出 STAFF_QUOTA_EXCEEDED")
    void createStaff_quotaExceeded() {
        when(accountRepository.existsByUsername("staff99")).thenReturn(false);
        when(accountRepository.countActiveBySchool(schoolId)).thenReturn(50);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createStaff(schoolId, "staff99",
                        "赵老师", null, null, operatorId));

        assertEquals(ErrorCode.STAFF_QUOTA_EXCEEDED, ex.getCode());
        verify(accountRepository, never()).insert(any());
    }

    @Test
    @DisplayName("用户名已被使用 → 抛出 USERNAME_ALREADY_EXISTS")
    void createStaff_usernameTaken() {
        when(accountRepository.existsByUsername("taken_name")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createStaff(schoolId, "taken_name",
                        "钱老师", null, null, operatorId));

        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, ex.getCode());
    }
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=AccountServiceTest -pl backend -q
```

期望: `BUILD SUCCESS`，全部 16 个测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/service/AccountServiceTest.java
git commit -m "test: add createStaff tests to AccountServiceTest"
```

---

## Task 3: AdmissionService — conditionalAdmission 边界用例

**Files:**
- Modify: `backend/src/test/java/com/campus/platform/service/AdmissionServiceTest.java`

在 `ConditionalAdmissionTests` 类中追加以下用例：

**Step 1: 追加 conditionalAdmission 边界用例**

```java
@Test
@DisplayName("有条件录取：名额配置不存在 → 抛出 QUOTA_NOT_FOUND")
void conditionalAdmission_quotaNotFound() {
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
    when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
            .thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.conditionalAdmission(pushId, majorId,
                    "数学≥120", LocalDate.now().plusDays(30), operatorId));

    assertEquals(ErrorCode.QUOTA_NOT_FOUND, ex.getCode());
}

@Test
@DisplayName("有条件录取：锁获取失败 → 抛出 DUPLICATE_ADMISSION")
void conditionalAdmission_lockFailed() {
    when(redisService.tryLock(any(), any())).thenReturn(false);

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.conditionalAdmission(pushId, majorId,
                    "数学≥120", LocalDate.now().plusDays(30), operatorId));

    assertEquals(ErrorCode.DUPLICATE_ADMISSION, ex.getCode());
}

@Test
@DisplayName("有条件录取：考生状态非 PENDING → 抛出 STATUS_TRANSITION_INVALID")
void conditionalAdmission_wrongStatus() {
    pendingPush.setStatus(CandidateStatus.ADMITTED.name());
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.conditionalAdmission(pushId, majorId,
                    "数学≥120", LocalDate.now().plusDays(30), operatorId));

    assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=AdmissionServiceTest -pl backend -q
```

期望: `BUILD SUCCESS`，3 个新测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/service/AdmissionServiceTest.java
git commit -m "test: add conditionalAdmission edge cases to AdmissionServiceTest"
```

---

## Task 4: AdmissionService — finalAdmission / rejectAdmission 边界用例

**Files:**
- Modify: `backend/src/test/java/com/campus/platform/service/AdmissionServiceTest.java`

**Step 1: 追加 finalAdmission 边界用例**

在 `FinalAdmissionTests` 类中追加：

```java
@Test
@DisplayName("终裁：考生不存在 → 抛出 CANDIDATE_NOT_FOUND")
void finalAdmission_candidateNotFound() {
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.finalAdmission(pushId, operatorId));

    assertEquals(ErrorCode.CANDIDATE_NOT_FOUND, ex.getCode());
}

@Test
@DisplayName("终裁：考生状态非 CONDITIONAL → 抛出 STATUS_TRANSITION_INVALID")
void finalAdmission_wrongStatus() {
    pendingPush.setStatus(CandidateStatus.PENDING.name()); // 不是 CONDITIONAL
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.finalAdmission(pushId, operatorId));

    assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
}
```

在 `RejectAdmissionTests` 类中追加：

```java
@Test
@DisplayName("拒绝 ADMITTED 考生 → 抛出 STATUS_TRANSITION_INVALID")
void rejectAdmitted_throws() {
    pendingPush.setStatus(CandidateStatus.ADMITTED.name());
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.rejectAdmission(pushId, "测试原因", operatorId));

    assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
}
```

在 `RevokeAdmissionTests` 类中追加：

```java
@Test
@DisplayName("撤销：考生不存在 → 抛出 CANDIDATE_NOT_FOUND")
void revokeAdmission_candidateNotFound() {
    when(candidatePushRepository.findById(pushId)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
            () -> admissionService.revokeAdmission(pushId, operatorId));

    assertEquals(ErrorCode.CANDIDATE_NOT_FOUND, ex.getCode());
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=AdmissionServiceTest -pl backend -q
```

期望: `BUILD SUCCESS`，全部 19 个测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/service/AdmissionServiceTest.java
git commit -m "test: add finalAdmission/rejectAdmission/revoke edge cases"
```

---

## Task 5: CandidateService — receivePush 状态边界用例

**Files:**
- Modify: `backend/src/test/java/com/campus/platform/service/CandidateServiceTest.java`

在 `ReceivePushTests` 类中追加：

**Step 1: 追加 receivePush 状态边界用例**

```java
@Test
@DisplayName("PENDING 状态重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
void pendingCandidate_duplicatePush_throws() {
    CandidatePush existing = new CandidatePush();
    existing.setPushId(UUID.randomUUID());
    existing.setSchoolId(schoolId);
    existing.setCandidateId("C001");
    existing.setStatus(CandidateStatus.PENDING.name()); // 已是进行中

    when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
            .thenReturn(Optional.of(existing));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> candidateService.receivePush(validRequest));

    assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
}

@Test
@DisplayName("CONDITIONAL 状态重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
void conditionalCandidate_duplicatePush_throws() {
    CandidatePush existing = new CandidatePush();
    existing.setPushId(UUID.randomUUID());
    existing.setSchoolId(schoolId);
    existing.setCandidateId("C001");
    existing.setStatus(CandidateStatus.CONDITIONAL.name()); // 有条件录取中

    when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
            .thenReturn(Optional.of(existing));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> candidateService.receivePush(validRequest));

    assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
}

@Test
@DisplayName("CONFIRMED 状态重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
void confirmedCandidate_duplicatePush_throws() {
    CandidatePush existing = new CandidatePush();
    existing.setPushId(UUID.randomUUID());
    existing.setSchoolId(schoolId);
    existing.setCandidateId("C001");
    existing.setStatus(CandidateStatus.CONFIRMED.name()); // 已确认报到

    when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
            .thenReturn(Optional.of(existing));

    BusinessException ex = assertThrows(BusinessException.class,
            () -> candidateService.receivePush(validRequest));

    assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=CandidateServiceTest -pl backend -q
```

期望: `BUILD SUCCESS`，全部 11 个测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/service/CandidateServiceTest.java
git commit -m "test: add receivePush duplicate push edge cases"
```

---

## Task 6: 新建 JwtTokenProviderTest

**Files:**
- Create: `backend/src/test/java/com/campus/platform/security/JwtTokenProviderTest.java`

**Step 1: 创建测试文件**

```java
package com.campus.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 * 不依赖 Spring 容器，直接 new 实例后调用 @PostConstruct 逻辑
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        // 反射调用 @PostConstruct init()
        var initMethod = JwtTokenProvider.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(provider);
    }

    private AccountPrincipal makeAdmin() {
        return new AccountPrincipal(
                UUID.randomUUID(),
                "OP_ADMIN",
                null,
                "管理员",
                UUID.randomUUID().toString()
        );
    }

    private AccountPrincipal makeSchoolUser(UUID schoolId) {
        return new AccountPrincipal(
                UUID.randomUUID(),
                "SCHOOL_ADMIN",
                schoolId,
                "招生负责人",
                UUID.randomUUID().toString()
        );
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("生成非空 Token 字符串")
        void generateToken_returnsNonNullString() {
            String token = provider.generateToken(makeAdmin());
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("OP_ADMIN Token 不含 schoolId claim")
        void generateToken_opAdmin_noSchoolId() {
            AccountPrincipal admin = makeAdmin();
            String token = provider.generateToken(admin);

            AccountPrincipal parsed = provider.parseToken(token);
            assertNotNull(parsed);
            assertNull(parsed.getSchoolId());
        }

        @Test
        @DisplayName("SCHOOL_ADMIN Token 携带 schoolId claim")
        void generateToken_schoolAdmin_hasSchoolId() {
            UUID schoolId = UUID.randomUUID();
            AccountPrincipal user = makeSchoolUser(schoolId);
            String token = provider.generateToken(user);

            AccountPrincipal parsed = provider.parseToken(token);
            assertNotNull(parsed);
            assertEquals(schoolId, parsed.getSchoolId());
        }
    }

    @Nested
    @DisplayName("parseToken")
    class ParseTokenTests {

        @Test
        @DisplayName("解析合法 Token → 返回 Principal，sub/role/realName 一致")
        void parseToken_validToken_returnsPrincipal() {
            AccountPrincipal original = makeAdmin();
            String token = provider.generateToken(original);

            AccountPrincipal parsed = provider.parseToken(token);

            assertNotNull(parsed);
            assertEquals(original.getAccountId(), parsed.getAccountId());
            assertEquals(original.getRole(), parsed.getRole());
            assertEquals(original.getRealName(), parsed.getRealName());
        }

        @Test
        @DisplayName("解析伪造/损坏 Token → 返回 null")
        void parseToken_invalidToken_returnsNull() {
            AccountPrincipal result = provider.parseToken("not.a.real.token");
            assertNull(result);
        }

        @Test
        @DisplayName("解析空字符串 → 返回 null（不抛出异常）")
        void parseToken_emptyString_returnsNull() {
            assertDoesNotThrow(() -> {
                AccountPrincipal result = provider.parseToken("");
                assertNull(result);
            });
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("合法 Token → 返回 true")
        void validateToken_valid_returnsTrue() {
            String token = provider.generateToken(makeAdmin());
            assertTrue(provider.validateToken(token));
        }

        @Test
        @DisplayName("非法 Token → 返回 false")
        void validateToken_invalid_returnsFalse() {
            assertFalse(provider.validateToken("garbage-token"));
        }
    }
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=JwtTokenProviderTest -pl backend -q
```

期望: `BUILD SUCCESS`，9 个测试全部通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/security/JwtTokenProviderTest.java
git commit -m "test: add JwtTokenProviderTest for generate/parse/validate"
```

---

## Task 7: 新建 ConditionalExpiryTaskTest

**Files:**
- Create: `backend/src/test/java/com/campus/platform/schedule/ConditionalExpiryTaskTest.java`

**Step 1: 创建测试文件**

```java
package com.campus.platform.schedule;

import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.service.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionalExpiryTaskTest {

    @Mock private CandidatePushRepository candidatePushRepository;
    @Mock private CandidateService candidateService;

    @InjectMocks private ConditionalExpiryTask task;

    @Test
    @DisplayName("无到期记录 → 不调用 handleConditionExpired")
    void processExpiredConditionals_noExpired_noAction() {
        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of());

        task.processExpiredConditionals();

        verify(candidateService, never()).handleConditionExpired(anyString(), anyString());
    }

    @Test
    @DisplayName("有 2 条到期记录 → 各调用一次 handleConditionExpired")
    void processExpiredConditionals_twoExpired_callsHandlerTwice() {
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();

        CandidatePush p1 = makeConditionalPush("C001", schoolA);
        CandidatePush p2 = makeConditionalPush("C002", schoolB);

        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of(p1, p2));
        doNothing().when(candidateService).handleConditionExpired(anyString(), anyString());

        task.processExpiredConditionals();

        verify(candidateService).handleConditionExpired("C001", schoolA.toString());
        verify(candidateService).handleConditionExpired("C002", schoolB.toString());
    }

    @Test
    @DisplayName("单条记录处理抛出异常 → 捕获后继续处理剩余记录")
    void processExpiredConditionals_oneThrows_continuesOthers() {
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();

        CandidatePush p1 = makeConditionalPush("C001", schoolA);
        CandidatePush p2 = makeConditionalPush("C002", schoolB);

        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of(p1, p2));

        // C001 处理时抛异常
        doThrow(new RuntimeException("DB 故障"))
                .when(candidateService).handleConditionExpired("C001", schoolA.toString());
        doNothing().when(candidateService)
                .handleConditionExpired("C002", schoolB.toString());

        // 不应向上抛出异常
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> task.processExpiredConditionals());

        // C002 仍被处理
        verify(candidateService).handleConditionExpired("C002", schoolB.toString());
    }

    private CandidatePush makeConditionalPush(String candidateId, UUID schoolId) {
        CandidatePush push = new CandidatePush();
        push.setPushId(UUID.randomUUID());
        push.setCandidateId(candidateId);
        push.setSchoolId(schoolId);
        push.setStatus(CandidateStatus.CONDITIONAL.name());
        push.setConditionDeadline(Instant.now().minusSeconds(3600));
        return push;
    }
}
```

**Step 2: 运行测试**

```bash
mvn test -Dtest=ConditionalExpiryTaskTest -pl backend -q
```

期望: `BUILD SUCCESS`，3 个测试通过。

**Step 3: Commit**

```bash
git add backend/src/test/java/com/campus/platform/schedule/ConditionalExpiryTaskTest.java
git commit -m "test: add ConditionalExpiryTaskTest for scheduled expiry processing"
```

---

## Task 8: 全量测试验证

**Step 1: 运行所有单元测试**

```bash
cd /Volumes/HP\ P900/Tech/yuanxiao/campus-platform/backend
mvn test -q
```

期望输出：

```
Tests run: ~65, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Step 2: 查看测试报告（可选）**

```bash
open target/surefire-reports/
```

**Step 3: 最终汇总 Commit（若有未提交内容）**

```bash
git add -A
git commit -m "test: comprehensive test suite - 35+ cases covering all service layers"
```

---

## 最终覆盖率汇总

| 测试文件 | 新增用例 | 累计用例 | 主要覆盖点 |
|---|---|---|---|
| AccountServiceTest | +7 | 16 | login×8, logout×1, createSchoolAdmin×3, createStaff×4 |
| AdmissionServiceTest | +7 | 18 | 直接/有条件/终裁/撤销/拒绝录取，边界+错误码全覆盖 |
| CandidateServiceTest | +3 | 11 | receivePush 所有中间态拒绝逻辑 |
| StatisticsServiceTest | 0 | 5 | 已覆盖 KPI/分布/排名 |
| JwtTokenProviderTest | +9 | 9 | generate/parse/validate 全路径 |
| ConditionalExpiryTaskTest | +3 | 3 | 定时任务空/批量/异常容忍 |
| **合计** | **+29** | **~62** | |
