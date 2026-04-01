package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.AdmissionQuota;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.repository.AdmissionQuotaRepository;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "名额管理")
@RestController
@RequestMapping("/api/v1/quotas")
@RequiredArgsConstructor
public class QuotaController {

    private final AdmissionQuotaRepository quotaRepository;
    private final MajorRepository majorRepository;

    @Operation(summary = "查询名额列表（分页）")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) UUID majorId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UUID schoolId = null;
        String role = SecurityContext.getRole();
        if (!AccountRole.OP_ADMIN.name().equals(role)) {
            schoolId = SecurityContext.getSchoolId();
        }

        List<AdmissionQuota> quotas = quotaRepository.findAll(schoolId, majorId, year);

        // 预加载专业名称
        var majorQ = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Major>();
        if (schoolId != null) majorQ.eq(Major::getSchoolId, schoolId);
        Map<UUID, String> majorNameMap = majorRepository.selectList(majorQ).stream()
                .collect(Collectors.toMap(Major::getMajorId, Major::getMajorName, (a, b) -> a));

        int total = quotas.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pageRecords = (fromIndex < total)
                ? quotas.subList(fromIndex, toIndex).stream()
                    .map(q -> {
                        Map<String, Object> m = new java.util.HashMap<>();
                        m.put("quotaId", q.getQuotaId().toString());
                        m.put("majorId", q.getMajorId().toString());
                        m.put("majorName", majorNameMap.getOrDefault(q.getMajorId(), ""));
                        m.put("year", q.getYear());
                        m.put("totalQuota", q.getTotalQuota());
                        m.put("enrolledCount", q.getAdmittedCount() != null ? q.getAdmittedCount() : 0);
                        m.put("reservedCount", q.getReservedCount() != null ? q.getReservedCount() : 0);
                        m.put("minScore", q.getMinScore());
                        m.put("maxScore", q.getMaxScore());
                        m.put("deadline", q.getDeadline());
                        m.put("startTime", q.getStartTime());
                        return m;
                    })
                    .collect(Collectors.toList())
                : List.of();

        return Result.ok(Map.of(
                "records", pageRecords,
                "total", total,
                "page", page,
                "pageSize", pageSize
        ));
    }

    @Operation(summary = "创建/更新名额配置")
    @PostMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> save(@RequestBody @Valid QuotaRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        int year = req.getYear() != null ? req.getYear() : LocalDate.now().getYear();
        Optional<AdmissionQuota> existing = quotaRepository
                .findBySchoolMajorYear(schoolId, req.getMajorId(), year);

        if (existing.isPresent()) {
            UUID quotaId = existing.get().getQuotaId();
            LambdaUpdateWrapper<AdmissionQuota> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(AdmissionQuota::getQuotaId, quotaId)
                    .set(AdmissionQuota::getTotalQuota, req.getTotalQuota())
                    .set(AdmissionQuota::getMinScore, req.getMinScore())
                    .set(AdmissionQuota::getMaxScore, req.getMaxScore())
                    .set(AdmissionQuota::getDeadline, req.getDeadline())
                    .set(AdmissionQuota::getStartTime, req.getStartTime());
            quotaRepository.update(null, wrapper);
        } else {
            AdmissionQuota quota = new AdmissionQuota();
            quota.setSchoolId(schoolId);
            quota.setMajorId(req.getMajorId());
            quota.setYear(year);
            quota.setTotalQuota(req.getTotalQuota());
            quota.setMinScore(req.getMinScore());
            quota.setMaxScore(req.getMaxScore());
            quota.setDeadline(req.getDeadline());
            quota.setStartTime(req.getStartTime());
            quota.setAdmittedCount(0);
            quota.setReservedCount(0);
            quota.setVersion(0);
            quotaRepository.insert(quota);
        }
        return Result.ok();
    }

    @Operation(summary = "更新名额")
    @PutMapping("/{quotaId}")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> update(@PathVariable UUID quotaId, @RequestBody @Valid QuotaUpdateRequest req) {
        LambdaUpdateWrapper<AdmissionQuota> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AdmissionQuota::getQuotaId, quotaId)
                .set(AdmissionQuota::getTotalQuota, req.getTotalQuota())
                .set(AdmissionQuota::getMinScore, req.getMinScore())
                .set(AdmissionQuota::getMaxScore, req.getMaxScore())
                .set(AdmissionQuota::getStartTime, req.getStartTime())
                .set(AdmissionQuota::getDeadline, req.getDeadline());
        quotaRepository.update(null, wrapper);
        return Result.ok();
    }

    @Data
    public static class QuotaRequest {
        @NotNull private UUID majorId;
        @Min(0) private Integer totalQuota;
        private Integer year;
        private Integer minScore;
        private Integer maxScore;
        private Instant startTime;
        private Instant deadline;
    }

    @Data
    public static class QuotaUpdateRequest {
        @Min(0) @NotNull private Integer totalQuota;
        private Integer minScore;
        private Integer maxScore;
        private Instant startTime;
        private Instant deadline;
    }
}
