package com.campus.platform.controller;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.SupplementRound;
import com.campus.platform.repository.SupplementRoundRepository;
import com.campus.platform.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Tag(name = "补录管理")
@RestController
@RequestMapping("/api/v1/supplement/rounds")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementRoundRepository supplementRoundRepository;

    @Operation(summary = "获取所有补录轮次")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<SupplementRound>> list() {
        return Result.ok(supplementRoundRepository.findAll());
    }

    @Operation(summary = "创建补录轮次")
    @PostMapping
    @RequireRole({"OP_ADMIN"})
    public Result<SupplementRound> create(@RequestBody @Valid CreateRoundRequest req) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant startInstant = LocalDateTime.parse(req.getStartTime(), fmt)
                .atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        Instant endInstant = LocalDateTime.parse(req.getEndTime(), fmt)
                .atZone(ZoneId.of("Asia/Shanghai")).toInstant();

        SupplementRound round = new SupplementRound();
        round.setRoundId(UUID.randomUUID());
        round.setRoundNumber(req.getRoundNumber());
        round.setStartTime(startInstant);
        round.setEndTime(endInstant);
        round.setRemark(req.getRemark());
        round.setStatus("UPCOMING");
        supplementRoundRepository.insert(round);
        return Result.ok(round);
    }

    @Operation(summary = "修改补录轮次状态")
    @PatchMapping("/{roundId}")
    @RequireRole({"OP_ADMIN"})
    public Result<Void> updateStatus(@PathVariable UUID roundId,
                                     @RequestBody UpdateStatusRequest req) {
        SupplementRound round = supplementRoundRepository.findById(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND, "补录轮次不存在"));
        round.setStatus(req.getStatus());
        supplementRoundRepository.updateById(round);
        return Result.ok();
    }

    @Data
    public static class CreateRoundRequest {
        private Integer roundNumber;
        @NotBlank private String startTime;
        @NotBlank private String endTime;
        private String remark;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank private String status;
    }
}
