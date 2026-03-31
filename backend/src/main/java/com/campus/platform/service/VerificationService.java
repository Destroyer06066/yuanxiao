package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.VerificationLog;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationLogRepository verificationLogRepository;
    private final CandidatePushRepository candidatePushRepository;

    /**
     * 查询核验记录列表
     */
    public IPage<Map<String, Object>> getLogs(UUID schoolId, UUID pushId, String result, int page, int pageSize) {
        Page<VerificationLog> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<VerificationLog> q = new LambdaQueryWrapper<>();
        q.eq(VerificationLog::getSchoolId, schoolId);
        if (pushId != null) {
            q.eq(VerificationLog::getPushId, pushId);
        }
        if (StringUtils.hasText(result)) {
            q.eq(VerificationLog::getResult, result);
        }
        q.orderByDesc(VerificationLog::getCreatedAt);
        IPage<VerificationLog> pageResult = verificationLogRepository.selectPage(p, q);

        // 附加考生姓名
        Page<Map<String, Object>> resultPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        resultPage.setRecords(pageResult.getRecords().stream().map(log -> {
            String candidateName = "";
            if (log.getPushId() != null) {
                candidateName = candidatePushRepository.findById(log.getPushId())
                        .map(CandidatePush::getCandidateName).orElse("");
            }
            return Map.<String, Object>of(
                    "verificationId", log.getVerificationId().toString(),
                    "pushId", log.getPushId() != null ? log.getPushId().toString() : "",
                    "candidateName", candidateName,
                    "action", log.getAction(),
                    "certificateNo", log.getCertificateNo() != null ? log.getCertificateNo() : "",
                    "result", log.getResult() != null ? log.getResult() : "",
                    "note", log.getNote() != null ? log.getNote() : "",
                    "operatorId", log.getOperatorId() != null ? log.getOperatorId().toString() : "",
                    "createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""
            );
        }).collect(java.util.stream.Collectors.toList()));
        return resultPage;
    }

    /**
     * 提交核验
     */
    @Transactional
    public void submit(UUID schoolId, String pushId, String operatorId, VerificationRequest req) {
        UUID pushUuid = UUID.fromString(pushId);
        CandidatePush push = candidatePushRepository.findById(pushUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        VerificationLog logEntry = new VerificationLog();
        logEntry.setVerificationId(UUID.randomUUID());
        logEntry.setSchoolId(schoolId);
        logEntry.setPushId(pushUuid);
        logEntry.setOperatorId(operatorId != null ? UUID.fromString(operatorId) : null);
        logEntry.setAction(req.getAction());
        logEntry.setCertificateNo(req.getCertificateNo());
        logEntry.setResult(req.getResult());
        logEntry.setNote(req.getNote());
        verificationLogRepository.insert(logEntry);
        log.info("提交核验: verificationId={}, pushId={}, result={}", logEntry.getVerificationId(), pushId, req.getResult());
    }

    /**
     * 批量核验
     */
    @Transactional
    public void batchSubmit(UUID schoolId, String operatorId, java.util.List<VerificationRequest> items) {
        for (VerificationRequest req : items) {
            submit(schoolId, req.getPushId(), operatorId, req);
        }
    }

    // ========== DTO ==========

    @lombok.Data
    public static class VerificationRequest {
        private String pushId;
        private String action;
        private String certificateNo;
        private String result;
        private String note;
    }
}
