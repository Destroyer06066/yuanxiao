package com.campus.platform.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.AdmissionQuota;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.SupplementInvitation;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.integration.outbound.RegistrationPlatformClient;
import com.campus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplementInvitationService {

    private final SupplementInvitationRepository supplementInvitationRepository;
    private final CandidatePushRepository candidatePushRepository;
    private final AdmissionQuotaRepository admissionQuotaRepository;
    private final MajorRepository majorRepository;
    private final SchoolConfigService schoolConfigService;
    private final RegistrationPlatformClient registrationPlatformClient;
    private final OperationLogService operationLogService;
    private final RedisService redisService;

    @Transactional
    public SupplementInvitation sendInvitation(UUID pushId, UUID majorId, String message, UUID operatorId) {
        if (!schoolConfigService.isSupplementModeTwo()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前未启用模式二（院校主动邀请）");
        }

        String lockKey = "supplement-invitation:" + pushId;
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

            // 创建补录邀请记录（不改变考生状态）
            SupplementInvitation invitation = new SupplementInvitation();
            invitation.setPushId(pushId);
            invitation.setCandidateId(push.getCandidateId());
            invitation.setSchoolId(push.getSchoolId());
            invitation.setInvitationMajorId(majorId);
            invitation.setStatus("INVITED");
            invitation.setSupplementRound(year); // 补录轮次用年份标识
            invitation.setMessage(message);
            invitation.setSentAt(Instant.now());
            invitation.setExpiresAt(expiresAt);
            invitation.setCreatedAt(Instant.now());
            invitation.setUpdatedAt(Instant.now());

            supplementInvitationRepository.insert(invitation);

            // 通知报名平台
            String majorName = majorRepository.findById(majorId).map(Major::getMajorName).orElse("");
            try {
                registrationPlatformClient.sendInvitationNotify(
                        push.getSchoolId(), push.getCandidateId(), push.getCandidateName(),
                        majorName, message, expiresAt.toString());
            } catch (Exception e) {
                log.warn("发送邀请通知失败: pushId={}", pushId, e);
            }

            operationLogService.log(pushId, "SEND_SUPPLEMENT_INVITATION", operatorId,
                    "发送补录邀请: " + majorName + "，留言: " + message);

            log.info("发送补录邀请: pushId={}, invitationId={}, majorId={}", pushId, invitation.getInvitationId(), majorId);
            return invitation;

        } finally {
            redisService.unlock(lockKey);
        }
    }

    @Transactional
    public int batchSendInvitation(List<UUID> pushIds, UUID majorId, String message, UUID operatorId) {
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

    public List<SupplementInvitation> listInvitations(UUID schoolId, String status, Integer round, String candidateKeyword) {
        return supplementInvitationRepository.listBySchoolId(schoolId, status, round, candidateKeyword);
    }

    public SupplementInvitation getById(UUID invitationId) {
        return supplementInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "邀请记录不存在"));
    }
}
