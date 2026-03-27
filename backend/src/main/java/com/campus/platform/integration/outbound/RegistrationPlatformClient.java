package com.campus.platform.integration.outbound;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * 报名平台 API 客户端（出站调用）
 *
 * 本服务负责向报名平台推送录取通知、拒绝通知等数据。
 * 开发测试阶段调用 Mock 服务。
 */
@Slf4j
@Service
public class RegistrationPlatformClient {

    @Value("${integration.registration-platform.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 推送录取通知
     */
    public void sendAdmissionNotify(UUID schoolId, String candidateId, String candidateName,
                                     String majorName, String remark) {
        String url = baseUrl + "/admission-notify";
        Map<String, Object> payload = Map.of(
                "schoolId", schoolId.toString(),
                "candidateId", candidateId,
                "candidateName", candidateName,
                "majorName", majorName,
                "remark", remark != null ? remark : "",
                "notifiedAt", java.time.Instant.now().toString()
        );
        post(url, payload, "录取通知推送");
    }

    /**
     * 推送有条件录取通知
     */
    public void sendConditionalNotify(UUID schoolId, String candidateId, String candidateName,
                                      String majorName, String conditionDesc,
                                      String conditionDeadline) {
        String url = baseUrl + "/conditional-notify";
        Map<String, Object> payload = Map.of(
                "schoolId", schoolId.toString(),
                "candidateId", candidateId,
                "candidateName", candidateName,
                "majorName", majorName,
                "conditionDesc", conditionDesc,
                "conditionDeadline", conditionDeadline,
                "notifiedAt", java.time.Instant.now().toString()
        );
        post(url, payload, "有条件录取通知推送");
    }

    /**
     * 推送拒绝通知
     */
    public void sendRejectNotify(UUID schoolId, String candidateId, String candidateName, String reason) {
        String url = baseUrl + "/reject-notify";
        Map<String, Object> payload = Map.of(
                "schoolId", schoolId.toString(),
                "candidateId", candidateId,
                "candidateName", candidateName,
                "reason", reason != null ? reason : "",
                "notifiedAt", java.time.Instant.now().toString()
        );
        post(url, payload, "拒绝通知推送");
    }

    /**
     * 推送报到确认
     */
    public void sendCheckinConfirm(UUID schoolId, String candidateId, String candidateName) {
        String url = baseUrl + "/checkin-confirm";
        Map<String, Object> payload = Map.of(
                "schoolId", schoolId.toString(),
                "candidateId", candidateId,
                "candidateName", candidateName,
                "confirmedAt", java.time.Instant.now().toString()
        );
        post(url, payload, "报到确认推送");
    }

    private void post(String url, Map<String, Object> payload, String logPrefix) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[{}] 成功: url={}", logPrefix, url);
            } else {
                log.warn("[{}] 失败: url={}, status={}", logPrefix, url, response.getStatusCode());
            }
        } catch (Exception e) {
            // 即使推送失败，业务操作仍继续，标记为待重试
            log.error("[{}] 异常: url={}, error={}", logPrefix, url, e.getMessage());
        }
    }
}
