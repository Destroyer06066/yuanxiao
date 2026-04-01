package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.Account;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "账号管理")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "账号列表")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        String currentRole = SecurityContext.getRole();
        // SCHOOL_STAFF 不允许访问账号列表
        if (AccountRole.SCHOOL_STAFF.name().equals(currentRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
        }

        UUID schoolId = null;
        if (!AccountRole.OP_ADMIN.name().equals(currentRole)) {
            // 非运营管理员只能看本校
            schoolId = SecurityContext.getSchoolId();
            if (schoolId == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
            }
        }

        var result = accountService.listAccounts(schoolId, role, status, keyword, page, pageSize);
        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    @Operation(summary = "创建账号")
    @PostMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN"})
    public Result<Map<String, Object>> create(@RequestBody @Valid AccountService.CreateAccountRequest req) {
        String currentRole = SecurityContext.getRole();
        UUID operatorId = SecurityContext.getAccountId();

        // SCHOOL_ADMIN 只能创建本校账号，且只能创建 SCHOOL_STAFF
        if (AccountRole.SCHOOL_ADMIN.name().equals(currentRole)) {
            if (!Objects.equals(req.getSchoolId(), SecurityContext.getSchoolId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权为其他院校创建账号");
            }
            if (!AccountRole.SCHOOL_STAFF.name().equals(req.getRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "院校管理员只能创建工作人员账号");
            }
            req.setSchoolId(SecurityContext.getSchoolId());
        }

        UUID accountId = accountService.createAccount(req, operatorId);
        return Result.ok(Map.of("accountId", accountId.toString()));
    }

    @Operation(summary = "编辑账号")
    @PutMapping("/{accountId}")
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN"})
    public Result<Void> update(@PathVariable String accountId,
                                @RequestBody @Valid AccountService.UpdateAccountRequest req) {
        String currentRole = SecurityContext.getRole();
        // 非 OP_ADMIN 只能编辑本校账号
        if (!AccountRole.OP_ADMIN.name().equals(currentRole)) {
            Account target = accountService.getAccountById(UUID.fromString(accountId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在"));
            if (!Objects.equals(target.getSchoolId(), SecurityContext.getSchoolId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑其他院校账号");
            }
            // 非 OP_ADMIN 不能改角色
            if (req.getRole() != null && !AccountRole.OP_ADMIN.name().equals(currentRole)) {
                req.setRole(null);
            }
        }
        accountService.updateAccount(accountId, req);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{accountId}/reset-password")
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN"})
    public Result<Void> resetPassword(@PathVariable String accountId) {
        accountService.resetPassword(accountId);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用账号")
    @PatchMapping("/{accountId}/status")
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN"})
    public Result<Void> toggleStatus(@PathVariable String accountId,
                                    @RequestBody @Valid StatusRequest req) {
        UUID currentId = SecurityContext.getAccountId();
        if (currentId.toString().equals(accountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能禁用自己的账号");
        }
        accountService.toggleStatus(accountId, req.getStatus());
        return Result.ok();
    }

    @Operation(summary = "批量导入账号")
    @PostMapping("/batch-import")
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN"})
    public Result<List<Map<String, Object>>> batchImport(@RequestBody @Valid List<AccountService.BatchImportItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "导入数据不能为空");
        }
        if (items.size() > 500) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "单次导入最多500条记录");
        }
        UUID operatorId = SecurityContext.getAccountId();
        var results = accountService.batchImportAccounts(items, operatorId);
        return Result.ok(results);
    }

    // ========== DTO ==========

    @Data
    public static class StatusRequest {
        @jakarta.validation.constraints.NotBlank
        private String status;
    }
}
