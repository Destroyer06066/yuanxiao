package com.campus.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 报名平台 Mock API
 *
 * 本 Mock 服务模拟报名平台的接口行为，用于本地开发测试。
 *
 * 接口列表（按 campus-platform 后端调用方向）：
 * 1. 录取通知    POST /mock-api/admission-notify
 * 2. 有条件录取  POST /mock-api/conditional-notify
 * 3. 拒绝通知    POST /mock-api/reject-notify
 * 4. 报到确认    POST /mock-api/checkin-confirm
 */
@Slf4j
@RestController
@RequestMapping("/mock-api")
public class MockController {

    /**
     * 接收院校管理平台的录取通知
     * 实际意义：报名平台收到录取通知后，展示给考生
     */
    @PostMapping("/admission-notify")
    public Map<String, Object> admissionNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到录取通知: {}", payload);
        return ok("ADMISSION_NOTIFIED", "录取通知已收到并展示给考生");
    }

    /**
     * 接收有条件录取通知
     */
    @PostMapping("/conditional-notify")
    public Map<String, Object> conditionalNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到有条件录取通知: {}", payload);
        return ok("CONDITIONAL_NOTIFIED", "有条件录取通知已收到");
    }

    /**
     * 接收拒绝通知
     */
    @PostMapping("/reject-notify")
    public Map<String, Object> rejectNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到拒绝通知: {}", payload);
        return ok("REJECTED", "拒绝通知已收到");
    }

    /**
     * 接收报到确认
     */
    @PostMapping("/checkin-confirm")
    public Map<String, Object> checkinConfirm(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到报到确认: {}", payload);
        return ok("CHECKIN_CONFIRMED", "报到确认已收到");
    }

    /**
     * 触发考生确认录取事件（模拟报名平台主动推送）
     * GET /mock-api/trigger/confirm?schoolId=xxx&candidateId=xxx
     * 用于测试录取互斥逻辑
     */
    @GetMapping("/trigger/confirm")
    public Map<String, Object> triggerConfirm(
            @RequestParam String schoolId,
            @RequestParam String candidateId) {
        log.info("[Mock] 模拟考生确认录取事件: schoolId={}, candidateId={}", schoolId, candidateId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "CANDIDATE_CONFIRMED");
        event.put("schoolId", schoolId);
        event.put("candidateId", candidateId);
        event.put("confirmedAt", java.time.Instant.now().toString());
        return ok("EVENT_TRIGGERED", event);
    }

    /**
     * 触发有条件录取到期事件
     */
    @GetMapping("/trigger/condition-expired")
    public Map<String, Object> triggerConditionExpired(
            @RequestParam String schoolId,
            @RequestParam String candidateId) {
        log.info("[Mock] 模拟条件到期事件: schoolId={}, candidateId={}", schoolId, candidateId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "CONDITION_EXPIRED");
        event.put("schoolId", schoolId);
        event.put("candidateId", candidateId);
        event.put("expiredAt", java.time.Instant.now().toString());
        return ok("EVENT_TRIGGERED", event);
    }

    private Map<String, Object> ok(String code, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("message", "success");
        r.put("data", data);
        r.put("timestamp", System.currentTimeMillis());
        return r;
    }
}
