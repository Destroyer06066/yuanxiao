package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.VerificationLog;
import com.campus.platform.entity.dto.CertificateVerificationRequest;
import com.campus.platform.entity.dto.CertificateVerificationResponse;
import com.campus.platform.integration.outbound.CertificateVerificationClient;
import com.campus.platform.repository.VerificationLogRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "核验管理")
@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final CertificateVerificationClient certificateVerificationClient;
    private final VerificationLogRepository verificationLogRepository;

    @Operation(summary = "证书核验")
    @PostMapping("/verify")
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<CertificateVerificationResponse> verify(
            @RequestBody @Valid CertificateVerificationRequest req) {
        // 调用报名平台API核验证书
        CertificateVerificationResponse response =
                certificateVerificationClient.verify(req.certificateNo(), req.verifyCode());

        // 记录核验日志
        VerificationLog logEntry = new VerificationLog();
        logEntry.setVerificationId(UUID.randomUUID());
        logEntry.setSchoolId(SecurityContext.getSchoolId());
        logEntry.setOperatorId(SecurityContext.getAccountId());
        logEntry.setAction("VERIFY");
        logEntry.setCertificateNo(req.certificateNo());
        logEntry.setResult(response.valid() ? "PASSED" : "FAILED");
        logEntry.setNote(response.message());
        verificationLogRepository.insert(logEntry);

        return Result.ok(response);
    }

    @Operation(summary = "核验记录列表")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String certificateNo,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        UUID schoolId = SecurityContext.getSchoolId();
        IPage<Map<String, Object>> pageResult =
                verificationService.getLogs(schoolId, certificateNo, result, page, pageSize);
        return Result.ok(Map.of(
                "records", pageResult.getRecords(),
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "pageSize", pageResult.getSize()
        ));
    }
}
