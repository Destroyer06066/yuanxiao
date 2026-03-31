package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "核验管理")
@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "核验记录列表")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) UUID pushId,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        UUID schoolId = SecurityContext.getSchoolId();
        IPage<Map<String, Object>> pageResult =
                verificationService.getLogs(schoolId, pushId, result, page, pageSize);
        return Result.ok(Map.of(
                "records", pageResult.getRecords(),
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "pageSize", pageResult.getSize()
        ));
    }

    @Operation(summary = "提交核验")
    @PostMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> submit(@RequestBody @Valid VerificationService.VerificationRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        String operatorId = SecurityContext.getAccountId() != null
                ? SecurityContext.getAccountId().toString() : null;
        verificationService.submit(schoolId, req.getPushId(), operatorId, req);
        return Result.ok();
    }

    @Operation(summary = "批量核验")
    @PostMapping("/batch")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> batchSubmit(@RequestBody @Valid BatchVerificationRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        String operatorId = SecurityContext.getAccountId() != null
                ? SecurityContext.getAccountId().toString() : null;
        verificationService.batchSubmit(schoolId, operatorId, req.getItems());
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class BatchVerificationRequest {
        @jakarta.validation.constraints.NotEmpty
        private List<VerificationService.VerificationRequest> items;
    }
}
