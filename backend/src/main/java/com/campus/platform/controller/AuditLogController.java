package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.AuditLog;
import com.campus.platform.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @Operation(summary = "审计日志列表（分页）")
    @GetMapping("/audit-logs")
    public Result<IPage<AuditLog>> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Page<AuditLog> p = new Page<>(page, pageSize);
        IPage<AuditLog> result = auditLogRepository.selectPaged(
                p, operatorName, action, startTime, endTime);
        return Result.ok(result);
    }
}
