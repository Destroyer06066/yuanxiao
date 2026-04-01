package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.SchoolBrochureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "招生简章")
@RestController
@RequestMapping("/api/v1/brochures")
@RequiredArgsConstructor
public class SchoolBrochureController {

    private final SchoolBrochureService brochureService;

    @Operation(summary = "获取本校招生简章")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<SchoolBrochureVO> get() {
        UUID schoolId = SecurityContext.getSchoolId();
        SchoolBrochure b = brochureService.getBySchool(schoolId);
        if (b == null) {
            return Result.ok(SchoolBrochureVO.empty());
        }
        return Result.ok(new SchoolBrochureVO(b.getBrochureId().toString(), b.getTitle(), b.getContent()));
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
        private String title;
        private String content;

        public static SchoolBrochureVO empty() {
            return new SchoolBrochureVO(null, "", "");
        }
    }
}
