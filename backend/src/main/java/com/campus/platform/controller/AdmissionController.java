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
