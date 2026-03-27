package com.campus.mock;

import com.campus.mock.data.MockDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * 报名平台 Mock API
 *
 * 本 Mock 服务模拟报名平台的接口行为，用于本地开发测试。
 *
 * A. 接收院校管理平台的推送（院校 → 报名平台方向）
 * B. 演示用 API（前端页面调用）
 */
@RestController
public class MockController {

    private static final Logger log = LoggerFactory.getLogger(MockController.class);

    private final MockDataStore dataStore;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integration.campus-backend.base-url}")
    private String campusBackendUrl;

    public MockController(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    // ==================== A. 接收院校管理平台的推送 ====================

    /**
     * 接收录取通知（院校侧直接录取/终裁录取时调用）
     */
    @PostMapping("/mock-api/admission-notify")
    public Map<String, Object> admissionNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到录取通知: {}", payload);

        String candidateId = str(payload, "candidateId");
        String schoolId = str(payload, "schoolId");
        String majorName = str(payload, "majorName");
        String remark = str(payload, "remark");

        dataStore.updatePushStatus(candidateId, schoolId, "ADMITTED", remark);

        String schoolName = MockDataStore.SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId))
                .map(MockDataStore.MockSchool::getName)
                .findFirst().orElse(schoolId);

        dataStore.addNotification(candidateId, new MockDataStore.MockNotification(
                "NOTIF-" + System.currentTimeMillis(), candidateId, "ADMISSION",
                "录取通知",
                "恭喜！" + schoolName + "正式录取您入读【" + majorName + "】专业。"
                        + (remark != null && !remark.isBlank() ? "备注：" + remark : "")
                        + "请在报名平台确认接受录取。",
                Instant.now().toString(), false
        ));

        return ok("ADMISSION_NOTIFIED", "录取通知已收到并展示给考生");
    }

    /**
     * 接收有条件录取通知
     */
    @PostMapping("/mock-api/conditional-notify")
    public Map<String, Object> conditionalNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到有条件录取通知: {}", payload);

        String candidateId = str(payload, "candidateId");
        String schoolId = str(payload, "schoolId");
        String majorName = str(payload, "majorName");
        String conditionDesc = str(payload, "conditionDesc");
        String conditionDeadline = str(payload, "conditionDeadline");

        dataStore.updatePushStatus(candidateId, schoolId, "CONDITIONAL", conditionDesc);

        String schoolName = MockDataStore.SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId))
                .map(MockDataStore.MockSchool::getName)
                .findFirst().orElse(schoolId);

        dataStore.addNotification(candidateId, new MockDataStore.MockNotification(
                "NOTIF-" + System.currentTimeMillis(), candidateId, "CONDITIONAL",
                "有条件录取通知",
                "恭喜！" + schoolName + "对您发出有条件录取通知。\n"
                        + "录取专业：" + majorName + "\n"
                        + "条件：" + conditionDesc + "\n"
                        + "截止时间：" + conditionDeadline + "\n"
                        + "请在截止时间前完成条件，并联系院校确认。",
                Instant.now().toString(), false
        ));

        return ok("CONDITIONAL_NOTIFIED", "有条件录取通知已收到");
    }

    /**
     * 接收拒绝通知
     */
    @PostMapping("/mock-api/reject-notify")
    public Map<String, Object> rejectNotify(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到拒绝通知: {}", payload);

        String candidateId = str(payload, "candidateId");
        String schoolId = str(payload, "schoolId");
        String reason = str(payload, "reason");

        dataStore.updatePushStatus(candidateId, schoolId, "REJECTED", reason);

        String schoolName = MockDataStore.SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId))
                .map(MockDataStore.MockSchool::getName)
                .findFirst().orElse(schoolId);

        dataStore.addNotification(candidateId, new MockDataStore.MockNotification(
                "NOTIF-" + System.currentTimeMillis(), candidateId, "REJECTION",
                "未录取通知",
                schoolName + "很遗憾地通知您，您的申请暂未通过。"
                        + (reason != null && !reason.isBlank() ? "原因：" + reason : ""),
                Instant.now().toString(), false
        ));

        return ok("REJECTED", "拒绝通知已收到");
    }

    /**
     * 接收报到确认
     */
    @PostMapping("/mock-api/checkin-confirm")
    public Map<String, Object> checkinConfirm(@RequestBody Map<String, Object> payload) {
        log.info("[Mock] 收到报到确认: {}", payload);
        return ok("CHECKIN_CONFIRMED", "报到确认已收到");
    }

    // ==================== B. 演示用 API（前端页面调用） ====================

    /**
     * 考生登录（演示用）
     */
    @PostMapping("/demo-api/login")
    public Map<String, Object> demoLogin(@RequestBody Map<String, Object> body) {
        String candidateId = str(body, "candidateId");
        log.info("[Demo] 考生登录: candidateId={}", candidateId);

        try {
            MockDataStore.MockCandidate c = dataStore.login(candidateId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("candidateId", c.getCandidateId());
            data.put("name", c.getName());
            data.put("englishName", c.getEnglishName());
            data.put("email", c.getEmail());
            data.put("nationality", c.getNationality());
            data.put("totalScore", c.getTotalScore());
            data.put("subjectScores", c.getSubjectScores());
            data.put("intention", c.getIntention());
            data.put("pushedAt", c.getPushedAt().toString());
            data.put("round", c.getRound());
            return ok(c.getCandidateId(), data);
        } catch (RuntimeException e) {
            return error("CANDIDATE_NOT_FOUND", e.getMessage());
        }
    }

    /**
     * 获取考生推送记录
     */
    @GetMapping("/demo-api/push-records")
    public Map<String, Object> getPushRecords(@RequestParam String candidateId) {
        List<MockDataStore.PushRecord> records = dataStore.getPushRecords(candidateId);
        return ok(records, records);
    }

    /**
     * 获取通知列表
     */
    @GetMapping("/demo-api/notifications")
    public Map<String, Object> getNotifications(@RequestParam String candidateId) {
        List<MockDataStore.MockNotification> notifications = dataStore.getNotifications(candidateId);
        int unread = dataStore.getUnreadCount(candidateId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notifications", notifications);
        data.put("unreadCount", unread);
        return ok(data, data);
    }

    /**
     * 标记通知已读
     */
    @PatchMapping("/demo-api/notifications/{notificationId}/read")
    public Map<String, Object> markRead(
            @PathVariable String notificationId,
            @RequestParam String candidateId) {
        dataStore.markNotificationRead(candidateId, notificationId);
        return ok(null, null);
    }

    /**
     * 标记所有已读
     */
    @PostMapping("/demo-api/notifications/read-all")
    public Map<String, Object> markAllRead(@RequestParam String candidateId) {
        dataStore.markAllNotificationsRead(candidateId);
        return ok(null, null);
    }

    /**
     * 考生确认录取（触发互斥逻辑）
     */
    @PostMapping("/demo-api/confirm")
    public Map<String, Object> confirmAdmission(@RequestBody Map<String, Object> body) {
        String candidateId = str(body, "candidateId");
        String schoolId = str(body, "schoolId");
        log.info("[Demo] 考生确认录取: candidateId={}, schoolId={}", candidateId, schoolId);

        try {
            MockDataStore.ConfirmResult result = dataStore.confirmAdmission(candidateId, schoolId);

            // 调用 campus-backend 触发互斥逻辑
            callCampusBackendConfirm(candidateId, schoolId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("confirmedSchool", result.getConfirmedSchool());
            data.put("invalidatedSchools", result.getInvalidatedSchools());
            data.put("message", "已确认接受" +
                    (result.getConfirmedSchool() != null ? result.getConfirmedSchool().getName() : "") + "的录取！");
            return ok("CONFIRMED", data);

        } catch (RuntimeException e) {
            return error("CONFIRM_FAILED", e.getMessage());
        }
    }

    /**
     * 考生放弃录取
     */
    @PostMapping("/demo-api/give-up")
    public Map<String, Object> giveUp(@RequestBody Map<String, Object> body) {
        String candidateId = str(body, "candidateId");
        String schoolId = str(body, "schoolId");
        dataStore.giveUpAdmission(candidateId, schoolId);
        return ok("GAVE_UP", "已放弃该录取");
    }

    /**
     * 考生确认已寄送材料
     */
    @PostMapping("/demo-api/material-sent")
    public Map<String, Object> materialSent(@RequestBody Map<String, Object> body) {
        String candidateId = str(body, "candidateId");
        String schoolId = str(body, "schoolId");
        String trackingNo = str(body, "trackingNo");
        String remark = str(body, "remark");
        dataStore.confirmMaterialSent(candidateId, schoolId, trackingNo, remark);
        return ok("MATERIAL_SENT", "已记录材料寄送信息");
    }

    /**
     * 获取模拟院校列表
     */
    @GetMapping("/demo-api/schools")
    public Map<String, Object> getSchools() {
        return ok(MockDataStore.SCHOOLS, MockDataStore.SCHOOLS);
    }

    /**
     * 获取所有演示考生账号
     */
    @GetMapping("/demo-api/demo-accounts")
    public Map<String, Object> getDemoAccounts() {
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (MockDataStore.MockCandidate c : MockDataStore.DEMO_CANDIDATES) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("candidateId", c.getCandidateId());
            m.put("name", c.getName());
            m.put("totalScore", c.getTotalScore());
            m.put("nationality", c.getNationality());
            accounts.add(m);
        }
        return ok(accounts, accounts);
    }

    // ==================== 触发事件 API（测试用） ====================

    /**
     * 触发考生确认录取事件
     */
    @GetMapping("/mock-api/trigger/confirm")
    public Map<String, Object> triggerConfirm(
            @RequestParam String schoolId,
            @RequestParam String candidateId) {
        log.info("[Mock] 模拟考生确认录取事件: schoolId={}, candidateId={}", schoolId, candidateId);
        callCampusBackendConfirm(candidateId, schoolId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "CANDIDATE_CONFIRMED");
        event.put("schoolId", schoolId);
        event.put("candidateId", candidateId);
        event.put("confirmedAt", Instant.now().toString());
        return ok("EVENT_TRIGGERED", event);
    }

    /**
     * 触发有条件录取到期事件
     */
    @GetMapping("/mock-api/trigger/condition-expired")
    public Map<String, Object> triggerConditionExpired(
            @RequestParam String schoolId,
            @RequestParam String candidateId) {
        log.info("[Mock] 模拟条件到期事件: schoolId={}, candidateId={}", schoolId, candidateId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "CONDITION_EXPIRED");
        event.put("schoolId", schoolId);
        event.put("candidateId", candidateId);
        event.put("expiredAt", Instant.now().toString());
        return ok("EVENT_TRIGGERED", event);
    }

    // ==================== 私有方法 ====================

    private void callCampusBackendConfirm(String candidateId, String schoolId) {
        try {
            String url = campusBackendUrl + "/api/v1/integration/candidate-confirmed";
            Map<String, Object> payload = Map.of(
                    "candidateId", candidateId,
                    "confirmedSchoolId", schoolId,
                    "confirmedAt", Instant.now().toString()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("[互斥] 回调 campus-backend 成功: status={}", response.getStatusCode());
        } catch (Exception e) {
            log.warn("[互斥] 回调 campus-backend 失败（不影响本地确认）: {}", e.getMessage());
        }
    }

    private Map<String, Object> ok(Object code, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 0);
        r.put("message", "success");
        r.put("data", data);
        r.put("timestamp", System.currentTimeMillis());
        return r;
    }

    private Map<String, Object> error(String code, String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", -1);
        r.put("message", msg);
        r.put("data", code);
        r.put("timestamp", System.currentTimeMillis());
        return r;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
