package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.SchoolBrochure;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.SchoolBrochureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "招生简章")
@RestController
@RequestMapping("/api/v1/brochures")
@RequiredArgsConstructor
public class SchoolBrochureController {

    private final SchoolBrochureService brochureService;

    @Operation(summary = "获取本校招生简章（或指定院校的简章 for OP_ADMIN）")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<SchoolBrochureVO> get(@RequestParam(required = false) UUID schoolId) {
        // OP_ADMIN 可以指定 schoolId 查看任意院校，不指定则查看自己的
        UUID contextSchoolId = SecurityContext.getSchoolId();
        if (!SecurityContext.isOpAdmin()) {
            schoolId = contextSchoolId;
        }
        if (schoolId == null) {
            return Result.ok(SchoolBrochureVO.empty());
        }
        SchoolBrochure b = brochureService.getBySchool(schoolId);
        if (b == null) {
            return Result.ok(SchoolBrochureVO.empty(schoolId.toString()));
        }
        return Result.ok(new SchoolBrochureVO(b.getBrochureId().toString(), b.getSchoolId().toString(), b.getTitle(), b.getContent()));
    }

    @Operation(summary = "获取所有院校的招生简章列表（仅 OP_ADMIN）")
    @GetMapping("/list")
    @RequireRole({"OP_ADMIN"})
    public Result<List<SchoolBrochureVO>> listAll() {
        List<SchoolBrochure> brochures = brochureService.getAll();
        List<SchoolBrochureVO> vos = brochures.stream()
                .map(b -> new SchoolBrochureVO(b.getBrochureId().toString(), b.getSchoolId().toString(), b.getTitle(), b.getContent()))
                .collect(Collectors.toList());
        return Result.ok(vos);
    }

    @Operation(summary = "保存招生简章")
    @PutMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> save(@RequestBody @Valid SchoolBrochureRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        brochureService.save(schoolId, req.getTitle(), req.getContent());
        return Result.ok();
    }

    @lombok.Data
    public static class SchoolBrochureRequest {
        private String title;
        private String content;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SchoolBrochureVO {
        private String brochureId;
        private String schoolId;
        private String title;
        private String content;

        public static SchoolBrochureVO empty() {
            return new SchoolBrochureVO(null, null, "", "");
        }

        public static SchoolBrochureVO empty(String schoolId) {
            return new SchoolBrochureVO(null, schoolId, "", "");
        }
    }
}
