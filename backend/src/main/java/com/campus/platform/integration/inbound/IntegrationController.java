package com.campus.platform.integration.inbound;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.service.AdmissionService;
import com.campus.platform.service.CandidateService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 接收报名平台推送的事件
 * 路径: /api/v1/integration/*
 * 特点：无鉴权（由报名平台侧保证安全），IP 白名单在 Nginx 层配置
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final CandidateService candidateService;
    private final AdmissionService admissionService;

    /**
     * API-IN-001: 报名平台推送考生成绩到本系统
     */
    @PostMapping("/push")
    public Result<Map<String, Object>> handlePush(@RequestBody CandidatePushRequest request) {
        log.info("[集成] 收到考生推送: candidateId={}, schoolId={}", request.candidateId, request.schoolId);
        CandidatePush push = candidateService.receivePush(request);
        return Result.ok(Map.of("pushId", push.getPushId().toString()));
    }

    /**
     * API-IN-002: 考生确认某院校录取，互斥处理
     * 报名平台推送此事件后，本系统处理：
     * 1. 本校考生状态 → CONFIRMED
     * 2. 其他发出录取通知的院校 → INVALIDATED + 名额释放
     */
    @PostMapping("/candidate-confirmed")
    public Result<Void> handleCandidateConfirmed(@RequestBody CandidateConfirmedRequest request) {
        log.info("[集成] 考生确认录取: candidateId={}, schoolId={}", request.candidateId, request.confirmedSchoolId);
        candidateService.handleCandidateConfirmed(request.candidateId, request.confirmedSchoolId);
        return Result.ok();
    }

    /**
     * API-IN-003: 有条件录取到期事件（定时任务触发或报名平台推送）
     */
    @PostMapping("/condition-expired")
    public Result<Void> handleConditionExpired(@RequestBody ConditionExpiredRequest request) {
        log.info("[集成] 条件到期: candidateId={}, schoolId={}", request.candidateId, request.schoolId);
        candidateService.handleConditionExpired(request.candidateId, request.schoolId);
        return Result.ok();
    }

    /**
     * API-IN-004: 补录邀请回调（考生接受/拒绝邀请）
     * 由报名平台在考生操作后调用
     */
    @PostMapping("/invitation-callback")
    public Result<Void> handleInvitationCallback(@RequestBody InvitationCallbackRequest request) {
        log.info("[集成] 邀请回调: pushId={}, choice={}", request.pushId, request.choice);

        UUID pushId = UUID.fromString(request.pushId);
        if ("ACCEPT".equalsIgnoreCase(request.choice)) {
            admissionService.acceptInvitation(pushId);
        } else if ("REJECT".equalsIgnoreCase(request.choice)) {
            admissionService.rejectInvitation(pushId);
        } else {
            return Result.fail(50002, "无效的选择，只能是 ACCEPT 或 REJECT");
        }
        return Result.ok();
    }

    // ========== DTO ==========

    public record CandidatePushRequest(
            String candidateId,
            String candidateName,
            String nationality,
            String idNumber,
            String email,
            BigDecimal totalScore,
            Map<String, BigDecimal> subjectScores,
            String intention,
            String schoolId,
            Integer round
    ) {}

    public record CandidateConfirmedRequest(
            String candidateId,
            String confirmedSchoolId,
            String confirmedAt
    ) {}

    public record ConditionExpiredRequest(
            String candidateId,
            String schoolId,
            String pushId,
            String expiredAt
    ) {}

    public record InvitationCallbackRequest(
            String pushId,
            String choice,  // ACCEPT 或 REJECT
            String respondedAt
    ) {}
}
