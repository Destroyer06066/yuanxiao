package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.SupplementRound;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.SupplementRoundRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
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
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "补录管理")
@RestController
@RequestMapping("/api/v1/supplement/rounds")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementRoundRepository supplementRoundRepository;
    private final CandidatePushRepository candidatePushRepository;

    @Operation(summary = "获取所有补录轮次")
    @GetMapping
    @RequireRole({"OP_ADMIN", "SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        List<SupplementRound> rounds = supplementRoundRepository.findAll();

        // 按 roundNumber 倒序
        rounds.sort((a, b) -> Integer.compare(b.getRoundNumber(), a.getRoundNumber()));

        // 批量统计各轮次考生数量
        List<Integer> roundNumbers = rounds.stream()
                .map(SupplementRound::getRoundNumber).distinct().toList();

        Map<Integer, RoundStats> statsMap = new HashMap<>();
        if (!roundNumbers.isEmpty()) {
            UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
            LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
            if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
            q.in(CandidatePush::getPushRound, roundNumbers);
            List<CandidatePush> all = candidatePushRepository.selectList(q);

            Map<Integer, List<CandidatePush>> byRound = all.stream()
                    .collect(Collectors.groupingBy(CandidatePush::getPushRound));

            for (Map.Entry<Integer, List<CandidatePush>> e : byRound.entrySet()) {
                int rn = e.getKey();
                List<CandidatePush> pushes = e.getValue();
                RoundStats s = new RoundStats();
                s.pushed = pushes.size();
                s.admitted = (int) pushes.stream()
                        .filter(p -> List.of(CandidateStatus.ADMITTED.name(),
                                CandidateStatus.CONDITIONAL.name()).contains(p.getStatus())).count();
                s.confirmed = (int) pushes.stream()
                        .filter(p -> List.of(CandidateStatus.CONFIRMED.name(),
                                CandidateStatus.MATERIAL_RECEIVED.name(),
                                CandidateStatus.CHECKED_IN.name()).contains(p.getStatus())).count();
                statsMap.put(rn, s);
            }
        }

        List<Map<String, Object>> result = rounds.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roundId", r.getRoundId());
            m.put("roundNumber", r.getRoundNumber());
            m.put("startTime", r.getStartTime());
            m.put("endTime", r.getEndTime());
            m.put("remark", r.getRemark());
            m.put("status", r.getStatus());
            RoundStats s = statsMap.getOrDefault(r.getRoundNumber(), new RoundStats());
            m.put("pushedCount", s.pushed);
            m.put("admittedCount", s.admitted);
            m.put("confirmedCount", s.confirmed);
            return m;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @Data
    private static class RoundStats {
        int pushed = 0;
        int admitted = 0;
        int confirmed = 0;
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
