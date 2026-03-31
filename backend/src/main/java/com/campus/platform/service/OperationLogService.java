package com.campus.platform.service;

import com.campus.platform.entity.OperationLog;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final AccountRepository accountRepository;

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
     * 获取考生的操作时间线
     */
    public List<OperationLog> getTimeline(UUID pushId) {
        return operationLogRepository.findByPushId(pushId);
    }
}
