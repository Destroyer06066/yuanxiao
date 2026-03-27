package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.ScoreLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "分数线配置")
@RestController
@RequestMapping("/api/v1/score-lines")
@RequiredArgsConstructor
public class ScoreLineController {

    private final ScoreLineService scoreLineService;

    @Operation(summary = "获取分数线列表")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        UUID schoolId = SecurityContext.getSchoolId();
        return Result.ok(scoreLineService.getBySchool(schoolId));
    }

    @Operation(summary = "创建分数线")
    @PostMapping
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> create(@RequestBody @Valid ScoreLineService.ScoreLineRequest req) {
        UUID schoolId = SecurityContext.getSchoolId();
        scoreLineService.create(schoolId, req);
        return Result.ok();
    }

    @Operation(summary = "编辑分数线")
    @PutMapping("/{lineId}")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> update(@PathVariable UUID lineId,
                                @RequestBody @Valid ScoreLineService.ScoreLineRequest req) {
        scoreLineService.update(lineId, req);
        return Result.ok();
    }

    @Operation(summary = "删除分数线")
    @DeleteMapping("/{lineId}")
    @RequireRole({"SCHOOL_ADMIN"})
    public Result<Void> delete(@PathVariable UUID lineId) {
        scoreLineService.delete(lineId);
        return Result.ok();
    }

}
