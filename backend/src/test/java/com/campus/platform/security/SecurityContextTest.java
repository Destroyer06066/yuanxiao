package com.campus.platform.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTest {

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    private AccountPrincipal makePrincipal(String role, UUID schoolId) {
        return new AccountPrincipal(
                UUID.randomUUID(),
                role,
                schoolId,
                "测试用户",
                UUID.randomUUID().toString(),
                List.of("major:read", "quota:read")
        );
    }

    @Nested
    @DisplayName("基础 set/get/clear 操作")
    class BasicOperations {

        @Test
        @DisplayName("set() 后 get() 返回相同的 principal")
        void set_then_get_returnsSamePrincipal() {
            AccountPrincipal principal = makePrincipal("OP_ADMIN", null);
            SecurityContext.set(principal);
            assertSame(principal, SecurityContext.get());
        }

        @Test
        @DisplayName("未设置时 get() 返回 null")
        void get_returnsNull_whenNothingSet() {
            assertNull(SecurityContext.get());
        }

        @Test
        @DisplayName("clear() 后 get() 返回 null")
        void clear_makesGetReturnNull() {
            SecurityContext.set(makePrincipal("OP_ADMIN", null));
            SecurityContext.clear();
            assertNull(SecurityContext.get());
        }
    }

    @Nested
    @DisplayName("便捷方法委托")
    class ConvenienceMethods {

        @Test
        @DisplayName("getAccountId() 返回 principal 的 accountId")
        void getAccountId_returnsCorrectValue() {
            AccountPrincipal principal = makePrincipal("SCHOOL_ADMIN", UUID.randomUUID());
            SecurityContext.set(principal);
            assertEquals(principal.getAccountId(), SecurityContext.getAccountId());
        }

        @Test
        @DisplayName("getRole() 返回 principal 的 role")
        void getRole_returnsCorrectValue() {
            AccountPrincipal principal = makePrincipal("SCHOOL_STAFF", UUID.randomUUID());
            SecurityContext.set(principal);
            assertEquals("SCHOOL_STAFF", SecurityContext.getRole());
        }

        @Test
        @DisplayName("getSchoolId() 返回 principal 的 schoolId")
        void getSchoolId_returnsCorrectValue() {
            UUID schoolId = UUID.randomUUID();
            AccountPrincipal principal = makePrincipal("SCHOOL_ADMIN", schoolId);
            SecurityContext.set(principal);
            assertEquals(schoolId, SecurityContext.getSchoolId());
        }

        @Test
        @DisplayName("无 principal 时 getAccountId() 返回 null")
        void getAccountId_returnsNull_whenNoPrincipal() {
            assertNull(SecurityContext.getAccountId());
        }

        @Test
        @DisplayName("无 principal 时 getRole() 返回 null")
        void getRole_returnsNull_whenNoPrincipal() {
            assertNull(SecurityContext.getRole());
        }

        @Test
        @DisplayName("无 principal 时 getSchoolId() 返回 null")
        void getSchoolId_returnsNull_whenNoPrincipal() {
            assertNull(SecurityContext.getSchoolId());
        }
    }

    @Nested
    @DisplayName("isOpAdmin 判断")
    class IsOpAdminTests {

        @Test
        @DisplayName("OP_ADMIN 角色 → 返回 true")
        void isOpAdmin_returnsTrue_forOpAdmin() {
            SecurityContext.set(makePrincipal("OP_ADMIN", null));
            assertTrue(SecurityContext.isOpAdmin());
        }

        @Test
        @DisplayName("非 OP_ADMIN 角色 → 返回 false")
        void isOpAdmin_returnsFalse_forOtherRoles() {
            SecurityContext.set(makePrincipal("SCHOOL_ADMIN", UUID.randomUUID()));
            assertFalse(SecurityContext.isOpAdmin());
        }

        @Test
        @DisplayName("无 principal → 返回 false")
        void isOpAdmin_returnsFalse_whenNoPrincipal() {
            assertFalse(SecurityContext.isOpAdmin());
        }
    }

    @Nested
    @DisplayName("线程隔离")
    class ThreadIsolation {

        @Test
        @DisplayName("一个线程设置 principal 不影响另一个线程")
        void differentThreads_haveIsolatedContexts() throws Exception {
            AccountPrincipal principal = makePrincipal("OP_ADMIN", null);
            SecurityContext.set(principal);

            AtomicReference<AccountPrincipal> otherThreadResult = new AtomicReference<>();
            Thread otherThread = new Thread(() -> otherThreadResult.set(SecurityContext.get()));
            otherThread.start();
            otherThread.join();

            // 当前线程有值
            assertSame(principal, SecurityContext.get());
            // 另一个线程为 null
            assertNull(otherThreadResult.get());
        }
    }
}
