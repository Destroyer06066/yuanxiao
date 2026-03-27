package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.AdmissionQuota;
import com.campus.platform.repository.AdmissionQuotaRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "名额管理")
@RestController
@RequestMapping("/api/v1/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final AdmissionQuotaRepository quotaRepository;

    @Operation(summary = "查询本校名额")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        UUID schoolId = SecurityContext.getSchoolId();
        if (schoolId == null) return Result.ok(List.of());
        List<AdmissionQuota> quotas = quotaRepository.findBySchoolId(schoolId);
        List<Map<String, Object>> result = quotas.stream().map(q -> Map.<String, Object>of(
                "quotaId", q.getQuotaId().toString(),
                "majorId", q.getMajorId().toString(),
                "year", q.getYear(),
                "totalQuota", q.getTotalQuota(),
                "admittedCount", q.getAdmittedCount(),
                "reservedCount", q.getReservedCount(),
                "remainQuota", q.getTotalQuota() - q.getAdmittedCount() - q.getReservedCount()
        )).collect(Collectors.toList());
        return Result.ok(result);
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
            AdmissionQuota quota = existing.get();
            quota.setTotalQuota(req.getTotalQuota());
            quotaRepository.updateById(quota);
        } else {
            AdmissionQuota quota = new AdmissionQuota();
            quota.setSchoolId(schoolId);
            quota.setMajorId(req.getMajorId());
            quota.setYear(year);
            quota.setTotalQuota(req.getTotalQuota());
            quota.setAdmittedCount(0);
            quota.setReservedCount(0);
            quota.setVersion(0);
            quotaRepository.insert(quota);
        }
        return Result.ok();
    }

    @Data
    public static class QuotaRequest {
        @NotNull private UUID majorId;
        @Min(0) private Integer totalQuota;
        private Integer year;
    }
}
