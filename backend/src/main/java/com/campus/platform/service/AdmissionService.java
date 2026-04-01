package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.*;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.integration.outbound.RegistrationPlatformClient;
import com.campus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * 录取操作核心服务：
 * 1. 直接录取（扣减名额，乐观锁）
 * 2. 有条件录取（预占名额）
 * 3. 终裁录取（预占转正式）
 * 4. 撤销录取（名额释放）
 * 5. 拒绝（名额释放）
 * 6. 补录邀请（模式二：院校主动邀请考生）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionService {

    private final CandidatePushRepository candidatePushRepository;
    private final AdmissionQuotaRepository admissionQuotaRepository;
    private final MajorRepository majorRepository;
    private final AccountRepository accountRepository;
    private final RegistrationPlatformClient registrationPlatformClient;
    private final RedisService redisService;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;
    private final SchoolConfigService schoolConfigService;

    /**
     * 直接录取
     */
    @Transactional
    public void directAdmission(UUID pushId, UUID majorId, String remark, UUID operatorId) {
        // 加分布式锁，防止重复操作
        String lockKey = "admission:" + pushId;
        if (!redisService.tryLock(lockKey, java.time.Duration.ofMinutes(1))) {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMISSION, "操作过于频繁，请稍后重试");
        }
        try {
            doAdmission(pushId, majorId, remark, operatorId, false);
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 有条件录取
     */
    @Transactional
    public void conditionalAdmission(UUID pushId, UUID majorId, String conditionDesc,
                                      LocalDate conditionDeadline, UUID operatorId) {
        String lockKey = "admission:" + pushId;
        if (!redisService.tryLock(lockKey, java.time.Duration.ofMinutes(1))) {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMISSION, "操作过于频繁，请稍后重试");
        }
        try {
            CandidatePush push = findAndValidate(pushId, CandidateStatus.PENDING);
            validateDeadline(push.getSchoolId());

            // 预占名额（reserved_count + 1）
            int year = LocalDate.now().getYear();
            Optional<AdmissionQuota> optQuota = admissionQuotaRepository
                    .findBySchoolMajorYear(push.getSchoolId(), majorId, year);
            if (optQuota.isEmpty()) {
                throw new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "未找到该专业名额配置");
            }
            AdmissionQuota quota = optQuota.get();
            quota.setReservedCount(quota.getReservedCount() + 1);
            admissionQuotaRepository.updateById(quota);

            // 更新考生状态
            push.setStatus(CandidateStatus.CONDITIONAL.name());
            push.setAdmissionMajorId(majorId);
            push.setConditionDesc(conditionDesc);
            push.setConditionDeadline(conditionDeadline.atStartOfDay(ZoneId.systemDefault()).toInstant());
            push.setOperatorId(operatorId);
            push.setOperatedAt(Instant.now());
            candidatePushRepository.updateById(push);

            // 通知报名平台
            registrationPlatformClient.sendConditionalNotify(
                    push.getSchoolId(), push.getCandidateId(), push.getCandidateName(),
                    majorRepository.findById(majorId).map(Major::getMajorName).orElse(""),
                    conditionDesc, conditionDeadline.toString());

            String majorName = majorRepository.findById(majorId).map(Major::getMajorName).orElse("");
            operationLogService.log(pushId, "CONDITIONAL", operatorId,
                    "有条件录取: " + majorName + "，条件: " + conditionDesc + "，截止: " + conditionDeadline);

            log.info("有条件录取: pushId={}, majorId={}", pushId, majorId);

        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 终裁录取（条件满足后转为正式录取）
     */
    @Transactional
    public void finalAdmission(UUID pushId, UUID operatorId) {
        CandidatePush push = findAndValidate(pushId, CandidateStatus.CONDITIONAL);

        int year = LocalDate.now().getYear();
        Optional<AdmissionQuota> optQuota = admissionQuotaRepository
                .findBySchoolMajorYear(push.getSchoolId(), push.getAdmissionMajorId(), year);
        if (optQuota.isPresent()) {
            AdmissionQuota quota = optQuota.get();
            if (quota.getReservedCount() > 0) {
                quota.setReservedCount(quota.getReservedCount() - 1);
            }
            quota.setAdmittedCount(quota.getAdmittedCount() + 1);
            admissionQuotaRepository.updateById(quota);
        }

        push.setStatus(CandidateStatus.ADMITTED.name());
        push.setOperatorId(operatorId);
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        registrationPlatformClient.sendAdmissionNotify(
                push.getSchoolId(), push.getCandidateId(), push.getCandidateName(),
                majorRepository.findById(push.getAdmissionMajorId())
                        .map(Major::getMajorName).orElse(""), "");

        operationLogService.log(pushId, "FINALIZE", operatorId, "条件满足，转为正式录取");

        // 站内通知
        String majorName = majorRepository.findById(push.getAdmissionMajorId())
                .map(Major::getMajorName).orElse("");
        notificationService.notifySchoolAdmins(
                push.getSchoolId(), pushId,
                "条件录取已确认",
                String.format("考生 %s 的条件已满足，正式录取至 %s", push.getCandidateName(), majorName));

        // 名额超 90% 预警
        optQuota.ifPresent(q -> checkQuotaOver90Alert(q, majorName));

        log.info("终裁录取: pushId={}", pushId);
    }

    /**
     * 撤销录取（截止时间前）
     */
    @Transactional
    public void revokeAdmission(UUID pushId, UUID operatorId) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.CONDITIONAL.name().equals(push.getStatus()) &&
            !CandidateStatus.ADMITTED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许撤销");
        }

        if (CandidateStatus.ADMITTED.name().equals(push.getStatus())) {
            // 正式录取撤销：释放 admitted_count
            releaseReservedQuota(push, true);
        } else {
            // 有条件录取撤销：释放 reserved_count
            releaseReservedQuota(push, false);
        }

        push.setStatus(CandidateStatus.PENDING.name());
        push.setConditionDesc(null);
        push.setConditionDeadline(null);
        push.setAdmissionMajorId(null);
        push.setOperatorId(operatorId);
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        operationLogService.log(pushId, "REVOKE", operatorId, "撤销录取");
        log.info("撤销录取: pushId={}", pushId);
    }

    /**
     * 拒绝录取
     */
    @Transactional
    public void rejectAdmission(UUID pushId, String reason, UUID operatorId) {
        CandidatePush push = findAndValidateAny(pushId,
                CandidateStatus.PENDING, CandidateStatus.CONDITIONAL);

        if (CandidateStatus.CONDITIONAL.name().equals(push.getStatus())) {
            releaseReservedQuota(push, false);
        }

        push.setStatus(CandidateStatus.REJECTED.name());
        push.setOperatorId(operatorId);
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        registrationPlatformClient.sendRejectNotify(
                push.getSchoolId(), push.getCandidateId(), push.getCandidateName(), reason);

        operationLogService.log(pushId, "REJECT", operatorId, "拒绝" + (reason != null ? "，原因: " + reason : ""));

        // 站内通知（记录在操作日志中）
        log.info("拒绝录取: pushId={}, reason={}", pushId, reason);
    }

    // ========== 私有辅助方法 ==========

    private void doAdmission(UUID pushId, UUID majorId, String remark, UUID operatorId, boolean isConditional) {
        CandidatePush push = findAndValidate(pushId, CandidateStatus.PENDING);
        validateDeadline(push.getSchoolId());

        int year = LocalDate.now().getYear();
        Optional<AdmissionQuota> optQuota = admissionQuotaRepository
                .findBySchoolMajorYear(push.getSchoolId(), majorId, year);

        if (optQuota.isEmpty()) {
            throw new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "未找到该专业名额配置");
        }

        AdmissionQuota quota = optQuota.get();

        // 乐观锁扣减名额
        int remaining = quota.getTotalQuota() - quota.getAdmittedCount() - quota.getReservedCount();
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.QUOTA_NOT_ENOUGH, "该专业名额已满");
        }

        quota.setAdmittedCount(quota.getAdmittedCount() + 1);
        admissionQuotaRepository.updateById(quota);

        // 更新考生状态
        push.setStatus(CandidateStatus.ADMITTED.name());
        push.setMajorId(majorId);
        push.setAdmissionMajorId(majorId);
        push.setAdmissionRemark(remark);
        push.setOperatorId(operatorId);
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        // 通知报名平台
        registrationPlatformClient.sendAdmissionNotify(
                push.getSchoolId(), push.getCandidateId(), push.getCandidateName(),
                majorRepository.findById(majorId).map(Major::getMajorName).orElse(""), remark != null ? remark : "");

        String majorName = majorRepository.findById(majorId).map(Major::getMajorName).orElse("");
        operationLogService.log(pushId, "ADMIT", operatorId, "录取专业: " + majorName + (remark != null ? "，备注: " + remark : ""));

        // 站内通知
        notificationService.notifySchoolAdmins(
                push.getSchoolId(), pushId,
                "考生录取通知",
                String.format("考生 %s 已被录取至 %s", push.getCandidateName(), majorName));

        // 名额超 90% 预警
        checkQuotaOver90Alert(quota, majorName);

        log.info("直接录取: pushId={}, majorId={}", pushId, majorId);
    }

    private CandidatePush findAndValidate(UUID pushId, CandidateStatus expectedStatus) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!expectedStatus.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "考生状态已变更，当前操作无效");
        }
        return push;
    }

    private CandidatePush findAndValidateAny(UUID pushId, CandidateStatus... statuses) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        boolean valid = false;
        for (CandidateStatus s : statuses) {
            if (s.name().equals(push.getStatus())) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许此操作");
        }
        return push;
    }

    private void validateDeadline(UUID schoolId) {
        // 从 school_config 读取截止时间，与当前时间比较
        // 简化：暂不实现，可按需扩展
    }

    private void releaseReservedQuota(CandidatePush push, boolean fromAdmitted) {
        if (push.getAdmissionMajorId() == null) return;
        int year = LocalDate.now().getYear();
        Optional<AdmissionQuota> opt = admissionQuotaRepository
                .findBySchoolMajorYear(push.getSchoolId(), push.getAdmissionMajorId(), year);
        if (opt.isEmpty()) return;
        AdmissionQuota quota = opt.get();
        if (fromAdmitted && quota.getAdmittedCount() > 0) {
            quota.setAdmittedCount(quota.getAdmittedCount() - 1);
        } else if (quota.getReservedCount() > 0) {
            quota.setReservedCount(quota.getReservedCount() - 1);
        }
        admissionQuotaRepository.updateById(quota);
    }

    /**
     * 名额超 90% 预警
     */
    private void checkQuotaOver90Alert(AdmissionQuota quota, String majorName) {
        int total = quota.getTotalQuota() != null ? quota.getTotalQuota() : 0;
        int admitted = quota.getAdmittedCount() != null ? quota.getAdmittedCount() : 0;
        int reserved = quota.getReservedCount() != null ? quota.getReservedCount() : 0;
        if (total > 0 && (double) (admitted + reserved) / total >= 0.9) {
            notificationService.onQuotaOver90(quota.getSchoolId(), majorName, admitted + reserved, total);
        }
    }

    // ========== 补录邀请相关方法（模式二）==========

    /**
     * 发送补录邀请（模式二）
     * @param pushId 考生推送ID
     * @param majorId 邀请录取专业
     * @param message 邀请留言
     * @param operatorId 操作人ID
     */
    @Transactional
    public void sendInvitation(UUID pushId, UUID majorId, String message, UUID operatorId) {
        if (!schoolConfigService.isSupplementModeTwo()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前未启用模式二（院校主动邀请）");
        }

        String lockKey = "admission:" + pushId;
        if (!redisService.tryLock(lockKey, java.time.Duration.ofMinutes(1))) {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMISSION, "操作过于频繁，请稍后重试");
        }
        try {
            CandidatePush push = candidatePushRepository.findById(pushId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

            // 验证状态：只有 PENDING/REJECTED/INVALIDATED 可以被邀请
            CandidateStatus currentStatus = CandidateStatus.valueOf(push.getStatus());
            if (!currentStatus.canBeInvited()) {
                throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许发送邀请");
            }

            int year = LocalDate.now().getYear();
            Optional<AdmissionQuota> optQuota = admissionQuotaRepository
                    .findBySchoolMajorYear(push.getSchoolId(), majorId, year);
            if (optQuota.isEmpty()) {
                throw new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "未找到该专业名额配置");
            }
            AdmissionQuota quota = optQuota.get();

            // 预占名额
            quota.setReservedCount(quota.getReservedCount() + 1);
            admissionQuotaRepository.updateById(quota);

            // 计算邀请过期时间
            int defaultDays = schoolConfigService.getInvitationDefaultDays();
            Instant expiresAt = Instant.now().plus(java.time.Duration.ofDays(defaultDays));

            // 更新考生状态
            push.setStatus(CandidateStatus.INVITED.name());
            push.setAdmissionMajorId(majorId);
            push.setInvitationSentAt(Instant.now());
            push.setInvitationExpiresAt(expiresAt);
            push.setInvitationMajorId(majorId);
            push.setInvitationMessage(message);
            push.setOperatorId(operatorId);
            push.setOperatedAt(Instant.now());
            candidatePushRepository.updateById(push);

            // 通知报名平台
            String majorName = majorRepository.findById(majorId).map(Major::getMajorName).orElse("");
            registrationPlatformClient.sendInvitationNotify(
                    push.getSchoolId(), push.getCandidateId(), push.getCandidateName(),
                    majorName, message, expiresAt.toString());

            operationLogService.log(pushId, "SEND_INVITATION", operatorId,
                    "发送补录邀请: " + majorName + "，留言: " + message);

            // 名额超 90% 预警
            checkQuotaOver90Alert(quota, majorName);

            log.info("发送补录邀请: pushId={}, majorId={}", pushId, majorId);
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 批量发送补录邀请（模式二）
     */
    @Transactional
    public int batchSendInvitation(java.util.List<UUID> pushIds, UUID majorId, String message, UUID operatorId) {
        if (!schoolConfigService.isSupplementModeTwo()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前未启用模式二（院校主动邀请）");
        }

        int successCount = 0;
        for (UUID pushId : pushIds) {
            try {
                sendInvitation(pushId, majorId, message, operatorId);
                successCount++;
            } catch (Exception e) {
                log.warn("批量发送邀请失败: pushId={}", pushId, e);
            }
        }
        return successCount;
    }

    /**
     * 考生接受邀请（回调）
     * @param pushId 考生推送ID
     */
    @Transactional
    public void acceptInvitation(UUID pushId) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.INVITED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不是已发出邀请");
        }

        // 检查邀请是否过期
        if (push.getInvitationExpiresAt() != null && push.getInvitationExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "邀请已过期");
        }

        // 状态变更为 PENDING，等待院校正式录取
        push.setStatus(CandidateStatus.PENDING.name());
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        operationLogService.log(pushId, "ACCEPT_INVITATION", null, "考生接受补录邀请");

        // 通知院校
        notificationService.notifySchoolAdmins(
                push.getSchoolId(), pushId,
                "考生接受补录邀请",
                String.format("考生 %s 已接受补录邀请，请及时处理", push.getCandidateName()));

        log.info("考生接受邀请: pushId={}", pushId);
    }

    /**
     * 考生拒绝邀请（回调）
     * @param pushId 考生推送ID
     */
    @Transactional
    public void rejectInvitation(UUID pushId) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.INVITED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不是已发出邀请");
        }

        // 释放预占名额
        releaseInvitationQuota(push);

        // 状态变更为 REJECTED
        push.setStatus(CandidateStatus.REJECTED.name());
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        operationLogService.log(pushId, "REJECT_INVITATION", null, "考生拒绝补录邀请");

        log.info("考生拒绝邀请: pushId={}", pushId);
    }

    /**
     * 处理邀请超时（定时任务调用）
     */
    @Transactional
    public void handleInvitationExpired(UUID pushId) {
        CandidatePush push = candidatePushRepository.findById(pushId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.INVITED.name().equals(push.getStatus())) {
            return; // 已经不是邀请状态，跳过
        }

        // 释放预占名额
        releaseInvitationQuota(push);

        // 状态变更为 REJECTED（邀请超时）
        push.setStatus(CandidateStatus.REJECTED.name());
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        operationLogService.log(pushId, "INVITATION_EXPIRED", null, "补录邀请超时");

        log.info("邀请超时处理: pushId={}", pushId);
    }

    private void releaseInvitationQuota(CandidatePush push) {
        if (push.getInvitationMajorId() == null) return;
        int year = LocalDate.now().getYear();
        Optional<AdmissionQuota> opt = admissionQuotaRepository
                .findBySchoolMajorYear(push.getSchoolId(), push.getInvitationMajorId(), year);
        if (opt.isEmpty()) return;
        AdmissionQuota quota = opt.get();
        if (quota.getReservedCount() > 0) {
            quota.setReservedCount(quota.getReservedCount() - 1);
            admissionQuotaRepository.updateById(quota);
        }
    }
}
