package com.campus.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.result.Result;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController 单元测试")
class AccountControllerTest {

    @Mock private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private AccountPrincipal opAdmin;

    @BeforeEach
    void setUp() {
        opAdmin = new AccountPrincipal(
                UUID.randomUUID(), "OP_ADMIN", null, "运营管理员", "jti-op", List.of("*"));
        SecurityContext.set(opAdmin);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    // ========== GET /api/v1/accounts ==========

    @Test
    @DisplayName("OP_ADMIN 获取全部账号列表")
    void list_opAdmin_returnsAllAccounts() {
        Page<Map<String, Object>> page = new Page<>(1, 20);
        Map<String, Object> accountMap = Map.of(
                "accountId", UUID.randomUUID().toString(),
                "username", "testuser",
                "realName", "测试用户",
                "role", "OP_ADMIN",
                "status", "ACTIVE"
        );
        page.setRecords(List.of(accountMap));
        page.setTotal(1);

        // OP_ADMIN: schoolId = null
        when(accountService.listAccounts(isNull(), isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(page);

        Result<Map<String, Object>> result = accountController.list(null, null, null, 1, 20);

        assertThat(result.getCode()).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getData().get("records");
        assertThat(records).hasSize(1);
        assertThat(result.getData().get("total")).isEqualTo(1L);
    }

    // ========== POST /api/v1/accounts ==========

    @Test
    @DisplayName("创建账号成功")
    void create_success_returnsAccountId() {
        UUID newAccountId = UUID.randomUUID();
        when(accountService.createAccount(any(AccountService.CreateAccountRequest.class), eq(opAdmin.getAccountId())))
                .thenReturn(newAccountId);

        AccountService.CreateAccountRequest req = new AccountService.CreateAccountRequest();
        req.setUsername("newuser");
        req.setRealName("新用户");
        req.setPhone("13900139000");
        req.setRole("SCHOOL_ADMIN");
        req.setSchoolId(UUID.randomUUID());

        Result<Map<String, Object>> result = accountController.create(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).containsEntry("accountId", newAccountId.toString());
    }

    // ========== PUT /api/v1/accounts/{id} ==========

    @Test
    @DisplayName("OP_ADMIN 更新账号成功")
    void update_opAdmin_success() {
        String accountId = UUID.randomUUID().toString();
        AccountService.UpdateAccountRequest req = new AccountService.UpdateAccountRequest();
        req.setRealName("更新姓名");
        req.setPhone("13800000001");

        doNothing().when(accountService).updateAccount(eq(accountId), any(AccountService.UpdateAccountRequest.class));

        Result<Void> result = accountController.update(accountId, req);

        assertThat(result.getCode()).isEqualTo(0);
        verify(accountService).updateAccount(eq(accountId), any());
    }

    // ========== POST /api/v1/accounts/{id}/reset-password ==========

    @Test
    @DisplayName("重置密码成功")
    void resetPassword_success() {
        String accountId = UUID.randomUUID().toString();
        doNothing().when(accountService).resetPassword(accountId);

        Result<Void> result = accountController.resetPassword(accountId);

        assertThat(result.getCode()).isEqualTo(0);
        verify(accountService).resetPassword(accountId);
    }

    // ========== PATCH /api/v1/accounts/{id}/status ==========

    @Test
    @DisplayName("切换账号状态成功")
    void toggleStatus_success() {
        String accountId = UUID.randomUUID().toString();
        doNothing().when(accountService).toggleStatus(accountId, "INACTIVE");

        AccountController.StatusRequest req = new AccountController.StatusRequest();
        req.setStatus("INACTIVE");

        Result<Void> result = accountController.toggleStatus(accountId, req);

        assertThat(result.getCode()).isEqualTo(0);
        verify(accountService).toggleStatus(accountId, "INACTIVE");
    }

    @Test
    @DisplayName("不能禁用自己的账号")
    void toggleStatus_cannotDisableSelf() {
        String ownAccountId = opAdmin.getAccountId().toString();
        AccountController.StatusRequest req = new AccountController.StatusRequest();
        req.setStatus("INACTIVE");

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.campus.platform.common.exception.BusinessException.class,
                () -> accountController.toggleStatus(ownAccountId, req));

        assertThat(ex.getCode()).isEqualTo(com.campus.platform.common.exception.ErrorCode.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo("不能禁用自己的账号");
    }

    @Test
    @DisplayName("SCHOOL_ADMIN schoolId 为空时无权访问账号列表")
    void list_schoolAdminWithNullSchoolId_forbidden() {
        AccountPrincipal schoolAdmin = new AccountPrincipal(
                UUID.randomUUID(), "SCHOOL_ADMIN", null, "院校管理员", "jti-school", List.of());
        SecurityContext.set(schoolAdmin);

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.campus.platform.common.exception.BusinessException.class,
                () -> accountController.list(null, null, null, 1, 20));

        assertThat(ex.getCode()).isEqualTo(com.campus.platform.common.exception.ErrorCode.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo("无权访问此资源");
    }
}
