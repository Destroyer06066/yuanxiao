package com.campus.platform.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.OperationLog;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AdmissionService;
import com.campus.platform.service.CandidateService;
import com.campus.platform.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final OperationLogService operationLogService;

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

    @Operation(summary = "全局搜索考生")
    @GetMapping("/search")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<List<CandidatePush>> search(@RequestParam String keyword) {
        // 按姓名或证件号或候选人ID模糊搜索
        // OP_ADMIN 查全部，SCHOOL 角色查本校
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(candidateService.searchCandidates(schoolId, keyword));
    }

    @Operation(summary = "考生操作时间线")
    @GetMapping("/{pushId}/timeline")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<List<OperationLog>> timeline(@PathVariable UUID pushId) {
        return Result.ok(operationLogService.getTimeline(pushId));
    }

    @Operation(summary = "终裁录取（条件满足转正式）")
    @PostMapping("/finalize")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> finalize(@RequestBody @Valid FinalizeRequest req) {
        admissionService.finalAdmission(req.getPushId(), SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "撤销录取")
    @PostMapping("/revoke")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> revoke(@RequestBody @Valid RevokeRequest req) {
        admissionService.revokeAdmission(req.getPushId(), SecurityContext.getAccountId());
        return Result.ok();
    }

    @Operation(summary = "导出考生列表 Excel")
    @GetMapping("/export")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public void export(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Float minScore,
            @RequestParam(required = false) Float maxScore,
            @RequestParam(required = false) String intentionKeyword,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pushTimeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pushTimeEnd,
            @RequestParam(required = false) UUID majorId,
            @RequestParam(required = false) Integer round,
            HttpServletResponse response) throws IOException {

        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();

        List<CandidatePush> records = candidateService.queryCandidatesForExport(
                schoolId, status, minScore, maxScore, intentionKeyword, nationality,
                pushTimeStart != null ? pushTimeStart.atStartOfDay(ZoneId.systemDefault()).toInstant() : null,
                pushTimeEnd != null ? pushTimeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null,
                majorId, round);

        // 转换为导出 DTO
        List<StudentExportDTO> dtoList = records.stream().map(r -> {
            StudentExportDTO dto = new StudentExportDTO();
            dto.setCandidateName(r.getCandidateName());
            dto.setNationality(r.getNationality());
            dto.setIdNumber(maskIdNumber(r.getIdNumber()));
            dto.setTotalScore(r.getTotalScore() != null ? r.getTotalScore().toString() : "-");
            dto.setIntention(r.getIntention());
            dto.setStatus(statusLabel(r.getStatus()));
            dto.setSchoolName(r.getSchoolName());
            dto.setMajorName(r.getAdmissionMajorName());
            dto.setPushedAt(r.getPushedAt() != null
                    ? r.getPushedAt().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
            dto.setOperatedAt(r.getOperatedAt() != null
                    ? r.getOperatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
            return dto;
        }).toList();

        String filename = "考生列表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        EasyExcel.write(response.getOutputStream(), StudentExportDTO.class)
                .sheet("考生列表")
                .doWrite(dtoList);
    }

    private static String maskIdNumber(String id) {
        if (id == null || id.length() < 6) return id;
        return id.substring(0, 4) + "****" + id.substring(id.length() - 4);
    }

    private static String statusLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "PENDING" -> "待处理";
            case "ADMITTED" -> "已录取";
            case "CONDITIONAL" -> "有条件录取";
            case "CONFIRMED" -> "已确认";
            case "MATERIAL_RECEIVED" -> "材料已收";
            case "CHECKED_IN" -> "已报到";
            case "REJECTED" -> "已拒绝";
            case "INVALIDATED" -> "已失效";
            default -> status;
        };
    }

    @Data
    public static class StudentExportDTO {
        @ExcelProperty("考生姓名") private String candidateName;
        @ExcelProperty("国籍") private String nationality;
        @ExcelProperty("证件号") private String idNumber;
        @ExcelProperty("总分") private String totalScore;
        @ExcelProperty("意向方向") private String intention;
        @ExcelProperty("状态") private String status;
        @ExcelProperty("院校") private String schoolName;
        @ExcelProperty("录取专业") private String majorName;
        @ExcelProperty("推送时间") private String pushedAt;
        @ExcelProperty("操作时间") private String operatedAt;
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

    @Data
    public static class FinalizeRequest {
        @NotNull private UUID pushId;
    }

    @Data
    public static class RevokeRequest {
        @NotNull private UUID pushId;
    }
}
