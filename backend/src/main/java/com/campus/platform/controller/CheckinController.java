package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "报到管理")
@RestController
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "获取报到列表（已录取/已确认/已收件/已报到的考生）")
    @GetMapping("/api/v1/checkins")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean materialReceived) {
        UUID schoolId = SecurityContext.getSchoolId();
        return Result.ok(checkinService.getCheckinList(schoolId, status, materialReceived));
    }

    @Operation(summary = "登记材料收件")
    @PostMapping("/api/v1/material-receive")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> receiveMaterial(@RequestBody @Valid ReceiveMaterialRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        String operatorId = SecurityContext.getAccountId() != null
                ? SecurityContext.getAccountId().toString() : null;
        checkinService.receiveMaterial(schoolId, req.getPushId(), operatorId, req.getNote());
        return Result.ok();
    }

    @Operation(summary = "确认报到")
    @PostMapping("/api/v1/checkin")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> checkin(@RequestBody @Valid CheckinRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        String operatorId = SecurityContext.getAccountId() != null
                ? SecurityContext.getAccountId().toString() : null;
        checkinService.doCheckin(schoolId, req.getPushId(), operatorId, req.getNote());
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class ReceiveMaterialRequest {
        @jakarta.validation.constraints.NotBlank
        private String pushId;
        private String note;
    }

    @Data
    public static class CheckinRequest {
        @jakarta.validation.constraints.NotBlank
        private String pushId;
        private String note;
    }
}
