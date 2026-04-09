package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.School;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.repository.SchoolRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "专业配置")
@RestController
@RequestMapping("/api/v1/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorRepository majorRepository;
    private final SchoolRepository schoolRepository;

    @Operation(summary = "查询本校专业列表")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // OP_ADMIN 可以指定 schoolId 查看任意院校，院校用户只能查看自己的
        UUID contextSchoolId = SecurityContext.getSchoolId();
        if (SecurityContext.isOpAdmin()) {
            // OP_ADMIN：使用请求参数中的 schoolId（可选），未指定则返回全部
        } else {
            schoolId = contextSchoolId;
        }
        if (schoolId == null && !SecurityContext.isOpAdmin()) {
            return Result.ok(Map.of("records", List.of(), "total", 0L, "page", page, "pageSize", pageSize));
        }

        List<Major> majors;
        if (SecurityContext.isOpAdmin() && schoolId == null) {
            // OP_ADMIN 且未指定 schoolId：返回所有专业
            majors = (status != null && !status.isEmpty())
                    ? majorRepository.findByStatus(status)
                    : majorRepository.findAll();
        } else {
            majors = (status != null && !status.isEmpty())
                    ? majorRepository.findBySchoolIdAndStatus(schoolId, status)
                    : majorRepository.findBySchoolId(schoolId);
        }

        int total = majors.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> records = majors.subList(from, to).stream().map(m -> {
            String schoolName = "";
            if (m.getSchoolId() != null) {
                schoolName = schoolRepository.findById(m.getSchoolId())
                        .map(School::getSchoolName).orElse("");
            }
            return Map.<String, Object>of(
                    "majorId", m.getMajorId().toString(),
                    "schoolId", m.getSchoolId() != null ? m.getSchoolId().toString() : "",
                    "schoolName", schoolName,
                    "majorName", m.getMajorName(),
                    "degreeLevel", m.getDegreeLevel(),
                    "status", m.getStatus(),
                    "createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : ""
            );
        }).collect(Collectors.toList());
        return Result.ok(Map.of("records", records, "total", (long) total, "page", page, "pageSize", pageSize));
    }

    @Operation(summary = "创建专业")
    @PostMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Map<String, Object>> create(@RequestBody @Valid CreateMajorRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        if (majorRepository.existsBySchoolIdAndName(schoolId, req.getMajorName())) {
            return Result.fail(20006, "该专业名称已存在");
        }
        Major major = new Major();
        major.setSchoolId(schoolId);
        major.setMajorName(req.getMajorName());
        major.setDegreeLevel(req.getDegreeLevel());
        major.setStatus("ACTIVE");
        majorRepository.insert(major);
        return Result.ok(Map.of("majorId", major.getMajorId().toString()));
    }

    @Operation(summary = "编辑专业")
    @PutMapping("/{majorId}")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> update(@PathVariable UUID majorId, @RequestBody @Valid UpdateMajorRequest req) {
        Major major = majorRepository.findById(majorId).orElseThrow();
        major.setMajorName(req.getMajorName());
        major.setDegreeLevel(req.getDegreeLevel());
        majorRepository.updateById(major);
        return Result.ok();
    }

    @Operation(summary = "停用/启用专业")
    @PatchMapping("/{majorId}/status")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> updateStatus(@PathVariable UUID majorId, @RequestBody StatusRequest req) {
        Major major = majorRepository.findById(majorId).orElseThrow();
        major.setStatus(req.getStatus());
        majorRepository.updateById(major);
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class CreateMajorRequest {
        @NotBlank @Size(max = 100) private String majorName;
        @NotBlank private String degreeLevel;
    }

    @Data
    public static class UpdateMajorRequest {
        @NotBlank @Size(max = 100) private String majorName;
        @NotBlank private String degreeLevel;
    }

    @Data
    public static class StatusRequest {
        @NotBlank private String status;
    }
}
