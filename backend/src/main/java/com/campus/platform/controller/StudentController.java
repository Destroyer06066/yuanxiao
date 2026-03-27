package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AdmissionService;
import com.campus.platform.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "考生管理")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final CandidateService candidateService;
    private final AdmissionService admissionService;

    @Operation(summary = "查询考生列表")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Float minScore,
            @RequestParam(required = false) Float maxScore,
            @RequestParam(required = false) String intentionKeyword,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pushTimeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pushTimeEnd,
            @RequestParam(required = false) UUID majorId,
            @RequestParam(required = false) Integer round,
            @RequestParam(defaultValue = "pushedAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UUID schoolId = SecurityContext.getSchoolId();
        if (schoolId == null && !SecurityContext.isOpAdmin()) {
            schoolId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // 简化处理
        }

        IPage<CandidatePush> result = candidateService.queryCandidates(
                schoolId, status, minScore, maxScore, intentionKeyword, nationality,
                pushTimeStart != null ? pushTimeStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant() : null,
                pushTimeEnd != null ? pushTimeEnd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant() : null,
                majorId, round, sort, order, page, pageSize);

        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    @Operation(summary = "考生详情")
    @GetMapping("/{pushId}")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<CandidatePush> detail(@PathVariable UUID pushId) {
        return candidateService.getById(pushId)
                .map(Result::ok)
                .orElse(Result.fail(30001, "考生记录不存在"));
    }

    @Operation(summary = "直接录取")
    @PostMapping("/admit")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> admit(@RequestBody @Valid AdmitRequest req) {
        admissionService.directAdmission(req.getPushId(), req.getMajorId(),
                req.getRemark(), SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "有条件录取")
    @PostMapping("/conditional")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> conditional(@RequestBody @Valid ConditionalRequest req) {
        admissionService.conditionalAdmission(req.getPushId(), req.getMajorId(),
                req.getConditionDesc(), req.getConditionDeadline(),
                SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "批量拒绝")
    @PostMapping("/batch-reject")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> batchReject(@RequestBody @Valid BatchRejectRequest req) {
        for (UUID pushId : req.getPushIds()) {
            admissionService.rejectAdmission(pushId, null, SecurityContext.getAccountId());
        }
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class AdmitRequest {
        @NotNull private UUID pushId;
        @NotNull private UUID majorId;
        private String remark;
    }

    @Data
    public static class ConditionalRequest {
        @NotNull private UUID pushId;
        @NotNull private UUID majorId;
        @NotBlank private String conditionDesc;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate conditionDeadline;
    }

    @Data
    public static class BatchRejectRequest {
        @NotNull private List<UUID> pushIds;
    }
}
