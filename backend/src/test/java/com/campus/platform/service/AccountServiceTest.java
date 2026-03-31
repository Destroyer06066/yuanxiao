package com.campus.platform.service;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.Account;
import com.campus.platform.entity.School;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.entity.enums.SchoolStatus;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.repository.SchoolRepository;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisService redisService;
    @Mock private RoleService roleService;

    @InjectMocks private AccountService accountService;

    private Account activeAccount;

    @BeforeEach
    void setUp() {
        activeAccount = new Account();
        activeAccount.setAccountId(UUID.randomUUID());
        activeAccount.setUsername("test_user");
        activeAccount.setPasswordHash("$2a$10$hashedpassword");
        activeAccount.setRole(AccountRole.SCHOOL_ADMIN.name());
        activeAccount.setSchoolId(UUID.randomUUID());
        activeAccount.setRealName("测试用户");
        activeAccount.setStatus("ACTIVE");
        activeAccount.setFailedLoginCount(0);
    }

    // ========== 登录测试 ==========

    @Nested
    @DisplayName("login")
    class LoginTests {

        @BeforeEach
        void setUpLoginMocks() {
            // 模拟 RoleService 返回 SCHOOL_ADMIN 的权限
            when(roleService.getRolePermissionStrings(AccountRole.SCHOOL_ADMIN.name()))
                    .thenReturn(List.of("account:read", "account:create", "account:edit", "account:disable",
                            "major:read", "major:create", "major:edit", "major:disable",
                            "quota:read", "quota:create", "quota:edit",
                            "verification:read", "verification:create",
                            "checkin:read", "checkin:material", "checkin:confirm",
                            "report:read"));
            when(roleService.getRolePermissionStrings(AccountRole.OP_ADMIN.name()))
                    .thenReturn(List.of("*"));
        }

        @Test
        @DisplayName("正确账号密码 → 登录成功，返回 Principal")
        void login_success() {
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
            when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");
            when(redisService.tryLock(any(), any())).thenReturn(true);
            doNothing().when(redisService).createSession(any(), any());

            AccountPrincipal principal = accountService.login("test_user", "password123", "127.0.0.1");

            assertNotNull(principal);
            assertEquals("测试用户", principal.getRealName());
            assertEquals(AccountRole.SCHOOL_ADMIN.name(), principal.getRole());
            verify(accountRepository).updateLoginFields(any(UUID.class), eq(0), isNull(), any(Instant.class));
        }

        @Test
        @DisplayName("用户名不存在 → 抛出 USERNAME_OR_PASSWORD_ERROR")
        void login_userNotFound() {
            when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("nonexistent", "password", "127.0.0.1"));

            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR, ex.getCode());
        }

        @Test
        @DisplayName("密码错误 → 失败计数 +1，剩余4次机会")
        void login_wrongPassword() {
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("test_user", "wrongpassword", "127.0.0.1"));

            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR, ex.getCode());
            assertTrue(ex.getMessage().contains("剩余"));
            verify(accountRepository).updateLoginFields(any(UUID.class), eq(1), isNull(), isNull());
        }

        @Test
        @DisplayName("密码连续错误5次 → 账号被锁定30分钟")
        void login_wrongPassword5Times_locksAccount() {
            activeAccount.setFailedLoginCount(4);
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("test_user", "wrongpassword", "127.0.0.1"));

            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR, ex.getCode());
            assertTrue(ex.getMessage().contains("锁定"));
            verify(accountRepository).updateLoginFields(
                    any(UUID.class), eq(5),
                    argThat(instant -> instant != null && instant.isAfter(Instant.now())),
                    isNull());
        }

        @Test
        @DisplayName("账号已停用(INACTIVE) → 抛出 ACCOUNT_DISABLED")
        void login_inactiveAccount() {
            activeAccount.setStatus("INACTIVE");
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("test_user", "password123", "127.0.0.1"));

            assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getCode());
        }

        @Test
        @DisplayName("账号已锁定(未过期) → 抛出 ACCOUNT_LOCKED")
        void login_lockedAccount() {
            activeAccount.setStatus("LOCKED");
            activeAccount.setLockedUntil(Instant.now().plusSeconds(1800));
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("test_user", "password123", "127.0.0.1"));

            assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getCode());
        }

        @Test
        @DisplayName("账号锁定已过期 → 自动解锁，允许登录")
        void login_expiredLock_autoUnlock() {
            activeAccount.setStatus("LOCKED");
            activeAccount.setLockedUntil(Instant.now().minusSeconds(10)); // 已过期
            activeAccount.setFailedLoginCount(3);
            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
            when(redisService.tryLock(any(), any())).thenReturn(true);
            doNothing().when(redisService).createSession(any(), any());

            AccountPrincipal principal = accountService.login("test_user", "password123", "127.0.0.1");

            assertNotNull(principal);
            // 已过期锁定在登录时自动重置
            verify(accountRepository).updateLoginFields(any(UUID.class), eq(0), isNull(), any(Instant.class));
        }

        @Test
        @DisplayName("OP_ADMIN 登录时跳过院校状态校验")
        void login_opAdmin_skipsSchoolStatusCheck() {
            activeAccount.setRole(AccountRole.OP_ADMIN.name());
            activeAccount.setSchoolId(null);
            when(accountRepository.findByUsername("op_admin")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
            when(redisService.tryLock(any(), any())).thenReturn(true);
            doNothing().when(redisService).createSession(any(), any());

            AccountPrincipal principal = accountService.login("op_admin", "password123", "127.0.0.1");

            assertNotNull(principal);
            // 不应该调用 schoolRepository
            verify(schoolRepository, never()).findById(any());
        }

        @Test
        @DisplayName("非OP_ADMIN登录时，若院校已停用则拒绝")
        void login_schoolDisabled_rejects() {
            UUID schoolId = UUID.randomUUID();
            activeAccount.setSchoolId(schoolId);
            activeAccount.setRole(AccountRole.SCHOOL_ADMIN.name());

            School disabledSchool = new School();
            disabledSchool.setSchoolId(schoolId);
            disabledSchool.setStatus(SchoolStatus.INACTIVE.name());

            when(accountRepository.findByUsername("test_user")).thenReturn(Optional.of(activeAccount));
            when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
            when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(disabledSchool));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> accountService.login("test_user", "password123", "127.0.0.1"));

            assertEquals(ErrorCode.SCHOOL_DISABLED, ex.getCode());
        }
    }

    // ========== 登出测试 ==========

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("登出时删除 Redis Session")
        void logout_deletesSession() {
            UUID accountId = UUID.randomUUID();
            doNothing().when(redisService).deleteSession(accountId);

            accountService.logout(accountId);

            verify(redisService).deleteSession(accountId);
        }
    }

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
                a.setAccountId(UUID.randomUUID());
                return 1;
            }).when(accountRepository).insert(any(Account.class));
            String password = accountService.createSchoolAdmin(schoolId, "blcu_admin",
                    "王招生", "13800138000", operatorId);

            assertNotNull(password);
            assertTrue(password.length() >= 12);
            verify(accountRepository).insert(argThat((Account a) ->
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
            verify(accountRepository, never()).insert((Account) any());
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
            verify(accountRepository, never()).insert((Account) any());
        }
    }

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

            String password = accountService.createStaff(schoolId, "staff01",
                    "李老师", "13900139000", null, operatorId);

            assertNotNull(password);
            assertTrue(password.length() >= 12);
            verify(accountRepository).insert(argThat((Account a) ->
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
            verify(accountRepository, never()).insert(any(Account.class));
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
}
