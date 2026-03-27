package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.repository.MajorRepository;
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

    @Operation(summary = "查询本校专业列表")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        UUID schoolId = SecurityContext.getSchoolId();
        if (schoolId == null) return Result.ok(List.of());
        List<Major> majors = majorRepository.findBySchoolId(schoolId);
        List<Map<String, Object>> result = majors.stream().map(m -> Map.<String, Object>of(
                "majorId", m.getMajorId().toString(),
                "majorName", m.getMajorName(),
                "degreeLevel", m.getDegreeLevel(),
                "status", m.getStatus()
        )).collect(Collectors.toList());
        return Result.ok(result);
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
