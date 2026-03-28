package com.campus.platform.security;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionAspectTest {

    @InjectMocks
    private PermissionAspect permissionAspect;

    @Mock
    private JoinPoint joinPoint;

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    private AccountPrincipal makePrincipal(String role, List<String> permissions) {
        return new AccountPrincipal(
                UUID.randomUUID(),
                role,
                UUID.randomUUID(),
                "测试用户",
                UUID.randomUUID().toString(),
                permissions
        );
    }

    @Nested
    @DisplayName("checkRole 角色校验")
    class CheckRoleTests {

        @Test
        @DisplayName("角色匹配 → 不抛异常")
        void matchingRole_noException() {
            SecurityContext.set(makePrincipal("OP_ADMIN", List.of("*")));

            RequireRole mockRole = mock(RequireRole.class);
            when(mockRole.value()).thenReturn(new String[]{"OP_ADMIN", "SCHOOL_ADMIN"});

            assertDoesNotThrow(() -> permissionAspect.checkRole(joinPoint, mockRole));
        }

        @Test
        @DisplayName("角色不匹配 → 抛出 FORBIDDEN")
        void wrongRole_throwsForbidden() {
            SecurityContext.set(makePrincipal("SCHOOL_STAFF", List.of("major:read")));

            RequireRole mockRole = mock(RequireRole.class);
            when(mockRole.value()).thenReturn(new String[]{"OP_ADMIN", "SCHOOL_ADMIN"});

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionAspect.checkRole(joinPoint, mockRole));
            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        @Test
        @DisplayName("未登录 → 抛出 UNAUTHORIZED")
        void noPrincipal_throwsUnauthorized() {
            RequireRole mockRole = mock(RequireRole.class);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionAspect.checkRole(joinPoint, mockRole));
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        }
    }

    @Nested
    @DisplayName("checkPermission 权限校验")
    class CheckPermissionTests {

        @Test
        @DisplayName("权限匹配 → 不抛异常")
        void matchingPermission_noException() {
            SecurityContext.set(makePrincipal("SCHOOL_ADMIN", List.of("major:read", "quota:read")));

            RequirePermission mockPerm = mock(RequirePermission.class);
            when(mockPerm.value()).thenReturn(new String[]{"major:read"});

            assertDoesNotThrow(() -> permissionAspect.checkPermission(joinPoint, mockPerm));
        }

        @Test
        @DisplayName("通配符 * → 通过任何权限检查")
        void wildcardPermission_passesAnyCheck() {
            SecurityContext.set(makePrincipal("OP_ADMIN", List.of("*")));

            RequirePermission mockPerm = mock(RequirePermission.class);

            assertDoesNotThrow(() -> permissionAspect.checkPermission(joinPoint, mockPerm));
        }

        @Test
        @DisplayName("缺少权限 → 抛出 FORBIDDEN")
        void missingPermission_throwsForbidden() {
            SecurityContext.set(makePrincipal("SCHOOL_STAFF", List.of("major:read")));

            RequirePermission mockPerm = mock(RequirePermission.class);
            when(mockPerm.value()).thenReturn(new String[]{"school:delete"});

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionAspect.checkPermission(joinPoint, mockPerm));
            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        @Test
        @DisplayName("未登录 → 抛出 UNAUTHORIZED")
        void noPrincipal_throwsUnauthorized() {
            RequirePermission mockPerm = mock(RequirePermission.class);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> permissionAspect.checkPermission(joinPoint, mockPerm));
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        }
    }
}
