package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.School;
import com.campus.platform.entity.enums.SchoolStatus;
import com.campus.platform.repository.SchoolRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "院校管理")
@RestController
@RequestMapping("/api/v1/admin/schools")
@RequireRole({"OP_ADMIN"})
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolRepository schoolRepository;

    @Operation(summary = "创建院校")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody @Valid CreateSchoolRequest req) {
        if (schoolRepository.existsByName(req.getSchoolName())) {
            return Result.fail(20002, "院校名称已存在");
        }
        School school = new School();
        school.setSchoolId(UUID.randomUUID());
        school.setSchoolName(req.getSchoolName());
        school.setSchoolShortName(req.getSchoolShortName());
        school.setProvince(req.getProvince());
        school.setSchoolType(req.getSchoolType());
        school.setContactName(req.getContactName());
        school.setContactPhone(req.getContactPhone());
        school.setContactEmail(req.getContactEmail());
        school.setWebsite(req.getWebsite());
        school.setRemark(req.getRemark());
        school.setStatus(SchoolStatus.ACTIVE.name());
        school.setCreatedBy(SecurityContext.getAccountId());
        schoolRepository.insert(school);
        return Result.ok(Map.of("schoolId", school.getSchoolId().toString()));
    }

    @Operation(summary = "查询院校列表")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String schoolType,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<School>(page, pageSize);
        IPage<School> result = schoolRepository.pageQuery(p, keyword, province, schoolType, status);
        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    @Operation(summary = "获取院校详情")
    @GetMapping("/{schoolId}")
    public Result<School> getById(@PathVariable UUID schoolId) {
        return Result.ok(schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("院校不存在")));
    }

    @Operation(summary = "编辑院校")
    @PutMapping("/{schoolId}")
    public Result<Void> update(@PathVariable UUID schoolId, @RequestBody @Valid UpdateSchoolRequest req) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("院校不存在"));
        if (!school.getSchoolName().equals(req.getSchoolName()) &&
                schoolRepository.existsByNameExcluding(req.getSchoolName(), schoolId)) {
            return Result.fail(20002, "院校名称已存在");
        }
        school.setSchoolName(req.getSchoolName());
        school.setSchoolShortName(req.getSchoolShortName());
        school.setProvince(req.getProvince());
        school.setSchoolType(req.getSchoolType());
        school.setContactName(req.getContactName());
        school.setContactPhone(req.getContactPhone());
        school.setContactEmail(req.getContactEmail());
        school.setWebsite(req.getWebsite());
        school.setRemark(req.getRemark());
        schoolRepository.updateById(school);
        return Result.ok();
    }

    @Operation(summary = "停用/启用院校")
    @PatchMapping("/{schoolId}/status")
    public Result<Void> updateStatus(@PathVariable UUID schoolId, @RequestBody StatusRequest req) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("院校不存在"));
        school.setStatus(req.getStatus());
        schoolRepository.updateById(school);
        return Result.ok();
    }

    // ========== DTO ==========

    @Data
    public static class CreateSchoolRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 100)
        private String schoolName;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 20)
        private String schoolShortName;

        @jakarta.validation.constraints.NotBlank
        private String province;

        @jakarta.validation.constraints.NotBlank
        private String schoolType;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 50)
        private String contactName;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "^1[3-9]\\d{9}$")
        private String contactPhone;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String contactEmail;

        private String website;
        private String remark;
    }

    @Data
    public static class UpdateSchoolRequest {
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100)
        private String schoolName;
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 20)
        private String schoolShortName;
        @jakarta.validation.constraints.NotBlank
        private String province;
        @jakarta.validation.constraints.NotBlank
        private String schoolType;
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 50)
        private String contactName;
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "^1[3-9]\\d{9}$")
        private String contactPhone;
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
        private String contactEmail;
        private String website;
        private String remark;
    }

    @Data
    public static class StatusRequest {
        @jakarta.validation.constraints.NotBlank
        private String status;
    }
}
