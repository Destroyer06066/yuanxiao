package com.campus.platform.service;

import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.OperationLog;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final AccountRepository accountRepository;
    private final CandidatePushRepository candidatePushRepository;

    /**
     * 记录操作日志
     */
    public void log(UUID pushId, String action, UUID operatorId, String remark) {
        String operatorName = accountRepository.findById(operatorId)
                .map(com.campus.platform.entity.Account::getRealName)
                .orElse(null);
        OperationLog log = new OperationLog();
        log.setLogId(UUID.randomUUID());
        log.setPushId(pushId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        log.setCreatedAt(Instant.now());
        operationLogRepository.insert(log);
    }

    /**
     * 获取考生的操作时间线（合并真实记录与模拟数据）
     * 策略：
     * 1. 如果无真实记录，生成完整的模拟时间线
     * 2. 如果有真实记录，以真实记录为主，但补充缺失的 PUSH 记录（基于推送时间）
     * 3. 确保时间线从 PUSH 开始，按时间排序
     */
    public List<OperationLog> getTimeline(UUID pushId) {
        List<OperationLog> logs = operationLogRepository.findByPushId(pushId);

        // 获取考生推送信息，用于补充 PUSH 记录
        CandidatePush push = candidatePushRepository.findById(pushId).orElse(null);

        if (logs == null || logs.isEmpty()) {
            // 无真实记录，生成完整的模拟时间线
            return generateMockTimeline(pushId);
        }

        // 有真实记录，检查是否缺少 PUSH 记录
        boolean hasPush = logs.stream().anyMatch(l -> "PUSH".equals(l.getAction()));

        if (!hasPush && push != null && push.getPushedAt() != null) {
            // 补充 PUSH mock 记录（使用真实的推送时间）
            List<OperationLog> result = new ArrayList<>();
            OperationLog pushLog = createMockLog(pushId, "PUSH", null, "考生提交成绩", push.getPushedAt());
            result.add(pushLog);
            result.addAll(logs);
            // PUSH 记录总是排在最前面，其他记录按时间排序
            result.sort((a, b) -> {
                // PUSH 总是排第一
                if ("PUSH".equals(a.getAction())) return -1;
                if ("PUSH".equals(b.getAction())) return 1;
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            });
            return result;
        }

        return logs;
    }

    /**
     * 根据考生状态生成模拟时间线
     */
    private List<OperationLog> generateMockTimeline(UUID pushId) {
        List<OperationLog> mockLogs = new ArrayList<>();
        Instant now = Instant.now();

        CandidatePush push = candidatePushRepository.findById(pushId).orElse(null);
        if (push == null) {
            return mockLogs;
        }

        String status = push.getStatus();
        Instant pushedAt = push.getPushedAt() != null ? push.getPushedAt() : now.minus(5, ChronoUnit.DAYS);

        // 推送是所有状态的起点
        mockLogs.add(createMockLog(pushId, "PUSH", null, "考生提交成绩", pushedAt));

        switch (status) {
            case "PENDING":
                // 无额外操作
                break;
            case "CONDITIONAL":
                mockLogs.add(createMockLog(pushId, "CONDITIONAL", push.getOperatorId(), "有条件录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                break;
            case "ADMITTED":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                break;
            case "CONFIRMED":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "CONFIRM", push.getOperatorId(), "考生已确认录取", pushedAt.plus(2, ChronoUnit.DAYS)));
                break;
            case "MATERIAL_RECEIVED":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "CONFIRM", push.getOperatorId(), "考生已确认录取", pushedAt.plus(2, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "MATERIAL_RECEIVE", push.getOperatorId(), "材料已收件", pushedAt.plus(3, ChronoUnit.DAYS)));
                break;
            case "CHECKED_IN":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "CONFIRM", push.getOperatorId(), "考生已确认录取", pushedAt.plus(2, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "MATERIAL_RECEIVE", push.getOperatorId(), "材料已收件", pushedAt.plus(3, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "CHECKIN", push.getOperatorId(), "已完成报到", pushedAt.plus(4, ChronoUnit.DAYS)));
                break;
            case "REJECTED":
                mockLogs.add(createMockLog(pushId, "REJECT", push.getOperatorId(), "已拒绝", pushedAt.plus(1, ChronoUnit.DAYS)));
                break;
            case "INVALIDATED":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "INVALIDATE", push.getOperatorId(), "录取已失效", pushedAt.plus(2, ChronoUnit.DAYS)));
                break;
            case "ENROLLED_ELSEWHERE":
                mockLogs.add(createMockLog(pushId, "ADMIT", push.getOperatorId(), "正式录取", pushedAt.plus(1, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "CONFIRM", push.getOperatorId(), "考生已确认录取", pushedAt.plus(2, ChronoUnit.DAYS)));
                mockLogs.add(createMockLog(pushId, "INVALIDATE", push.getOperatorId(), "已被他校录取", pushedAt.plus(3, ChronoUnit.DAYS)));
                break;
            default:
                break;
        }

        return mockLogs;
    }

    private OperationLog createMockLog(UUID pushId, String action, UUID operatorId, String remark, Instant createdAt) {
        OperationLog log = new OperationLog();
        log.setLogId(UUID.randomUUID());
        log.setPushId(pushId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorId != null ? "系统管理员" : "考生");
        log.setRemark(remark);
        log.setCreatedAt(createdAt);
        return log;
    }
}
