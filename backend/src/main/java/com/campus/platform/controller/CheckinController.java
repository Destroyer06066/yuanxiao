package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "报到管理")
@RestController
@RequiredArgsConstructor
public class CheckinController {

    private final CandidatePushRepository candidatePushRepository;
    private final MajorRepository majorRepository;

    @Operation(summary = "获取报到列表")
    @GetMapping("/api/v1/checkins")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<List<Map<String, Object>>> list() {
        UUID schoolId = SecurityContext.getSchoolId();
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        q.eq(CandidatePush::getSchoolId, schoolId)
         .in(CandidatePush::getStatus, List.of(
                 CandidateStatus.CONFIRMED.name(),
                 CandidateStatus.MATERIAL_RECEIVED.name(),
                 CandidateStatus.CHECKED_IN.name()))
         .orderByDesc(CandidatePush::getOperatedAt);

        List<CandidatePush> pushes = candidatePushRepository.selectList(q);

        List<Map<String, Object>> result = pushes.stream().map(p -> {
            String majorName = p.getMajorId() != null
                    ? majorRepository.findById(p.getMajorId())
                            .map(m -> m.getMajorName()).orElse("")
                    : "";
            String statusDesc = CandidateStatus.valueOf(p.getStatus()).getDescription();
            return Map.<String, Object>of(
                    "pushId", p.getPushId().toString(),
                    "candidateName", p.getCandidateName(),
                    "majorName", majorName,
                    "status", p.getStatus(),
                    "statusDesc", statusDesc,
                    "receiveTime", (p.getStatus().equals(CandidateStatus.MATERIAL_RECEIVED.name()) || p.getStatus().equals(CandidateStatus.CHECKED_IN.name()))
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : "",
                    "checkinTime", p.getStatus().equals(CandidateStatus.CHECKED_IN.name())
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : ""
            );
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @Operation(summary = "登记收件")
    @PostMapping("/api/v1/material-receive")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> receiveMaterial(@RequestBody PushIdRequest req) {
        CandidatePush push = candidatePushRepository.findById(UUID.fromString(req.getPushId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));
        if (!CandidateStatus.CONFIRMED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许登记收件");
        }
        push.setStatus(CandidateStatus.MATERIAL_RECEIVED.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(SecurityContext.getAccountId());
        candidatePushRepository.updateById(push);
        return Result.ok();
    }

    @Operation(summary = "确认报到")
    @PostMapping("/api/v1/checkin")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Void> checkin(@RequestBody PushIdRequest req) {
        CandidatePush push = candidatePushRepository.findById(UUID.fromString(req.getPushId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));
        if (!CandidateStatus.MATERIAL_RECEIVED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "请先登记收件");
        }
        push.setStatus(CandidateStatus.CHECKED_IN.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(SecurityContext.getAccountId());
        candidatePushRepository.updateById(push);
        return Result.ok();
    }

    @Data
    public static class PushIdRequest {
        private String pushId;
    }
}
