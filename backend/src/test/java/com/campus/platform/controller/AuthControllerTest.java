package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.Account;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.JwtTokenProvider;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AccountService;
import com.campus.platform.service.RedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    @Mock private AccountService accountService;
    @Mock private RedisService redisService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountRepository accountRepository;

    @InjectMocks
    private AuthController authController;

    private AccountPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AccountPrincipal(
                UUID.randomUUID(), "OP_ADMIN", null, "测试管理员", "jti-123", List.of("*"));
        SecurityContext.set(principal);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    // ========== POST /api/v1/auth/login ==========

    @Test
    @DisplayName("登录成功 - 返回 token 和用户信息")
    void login_success_returnsTokenAndUserInfo() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("admin");
        req.setPassword("Aa123456!");

        AccountPrincipal loginPrincipal = new AccountPrincipal(
                UUID.randomUUID(), "OP_ADMIN", null, "管理员", "jti-abc", List.of("*"));
        when(accountService.login(eq("admin"), eq("Aa123456!"), eq(""))).thenReturn(loginPrincipal);
        when(jwtTokenProvider.generateToken(loginPrincipal)).thenReturn("mock-jwt-token");

        Result<Map<String, Object>> result = authController.login(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).containsEntry("accessToken", "mock-jwt-token");
        assertThat(result.getData()).containsEntry("role", "OP_ADMIN");
        assertThat(result.getData()).containsEntry("realName", "管理员");
        assertThat(result.getData()).containsEntry("requirePasswordChange", false);
        verify(accountService).login("admin", "Aa123456!", "");
    }

    @Test
    @DisplayName("登录成功 - 院校账号返回 schoolId")
    void login_success_schoolAccount_returnsSchoolId() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("school_admin");
        req.setPassword("Aa123456!");

        UUID schoolId = UUID.randomUUID();
        AccountPrincipal loginPrincipal = new AccountPrincipal(
                UUID.randomUUID(), "SCHOOL_ADMIN", schoolId, "院校管理员", "jti-xyz", List.of());
        when(accountService.login(eq("school_admin"), eq("Aa123456!"), eq(""))).thenReturn(loginPrincipal);
        when(jwtTokenProvider.generateToken(loginPrincipal)).thenReturn("school-token");

        Result<Map<String, Object>> result = authController.login(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).containsEntry("schoolId", schoolId.toString());
        assertThat(result.getData()).containsEntry("role", "SCHOOL_ADMIN");
    }

    @Test
    @DisplayName("登录失败 - 用户名密码错误返回错误码")
    void login_wrongCredentials_returnsError() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        when(accountService.login(eq("admin"), eq("wrong"), eq("")))
                .thenThrow(new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误"));

        assertThatThrownBy(() -> authController.login(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
                });
    }

    // ========== GET /api/v1/auth/me ==========

    @Test
    @DisplayName("获取当前用户信息 - 已登录返回用户信息")
    void me_loggedIn_returnsUserInfo() {
        Result<Map<String, Object>> result = authController.me();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).containsEntry("accountId", principal.getAccountId().toString());
        assertThat(result.getData()).containsEntry("role", "OP_ADMIN");
        assertThat(result.getData()).containsEntry("realName", "测试管理员");
    }

    @Test
    @DisplayName("获取当前用户信息 - 未登录抛出异常")
    void me_notLoggedIn_throwsUnauthorized() {
        SecurityContext.clear();

        assertThatThrownBy(() -> authController.me())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                });
    }

    // ========== POST /api/v1/auth/logout ==========

    @Test
    @DisplayName("登出成功 - 返回 ok")
    void logout_success_returnsOk() {
        Result<Void> result = authController.logout();

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("success");
        verify(accountService).logout(principal.getAccountId());
    }
}
