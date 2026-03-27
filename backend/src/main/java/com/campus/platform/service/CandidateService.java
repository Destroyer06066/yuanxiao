package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.*;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.integration.inbound.IntegrationController.*;
import com.campus.platform.integration.outbound.RegistrationPlatformClient;
import com.campus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 考生管理核心服务：
 * 1. 接收报名平台推送（receivePush）
 * 2. 考生确认录取互斥（handleCandidateConfirmed）
 * 3. 条件到期处理（handleConditionExpired）
 * 4. 列表查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidatePushRepository candidatePushRepository;
    private final AdmissionQuotaRepository admissionQuotaRepository;
    private final MajorRepository majorRepository;
    private final AccountRepository accountRepository;
    private final RegistrationPlatformClient registrationPlatformClient;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    /**
     * 接收报名平台推送的考生成绩
     */
    @Transactional
    public CandidatePush receivePush(CandidatePushRequest req) {
        UUID schoolId = UUID.fromString(req.schoolId());

        // 防重复推送：同一考生+同一院校，如果已是终态则重新开启
        Optional<CandidatePush> existing = candidatePushRepository
                .findBySchoolAndCandidate(schoolId, req.candidateId());

        CandidatePush push;
        if (existing.isPresent()) {
            push = existing.get();
            if (!push.getStatus().equals(CandidateStatus.PENDING.name()) &&
                !push.getStatus().equals(CandidateStatus.REJECTED.name()) &&
                !push.getStatus().equals(CandidateStatus.INVALIDATED.name())) {
                throw new BusinessException(ErrorCode.INTEGRATION_PUSH_ERROR,
                        "考生已在录取流程中，无法重新推送");
            }
            // 重新开启
            push.setStatus(CandidateStatus.PENDING.name());
            push.setTotalScore(req.totalScore() != null ? req.totalScore() : null);
            push.setSubjectScores(req.subjectScores());
            push.setIntention(req.intention());
            push.setPushRound(req.round() != null ? req.round() : 0);
            push.setPushedAt(Instant.now());
            push.setMajorId(null);
            push.setAdmissionMajorId(null);
            push.setAdmissionRemark(null);
            push.setConditionDesc(null);
            push.setConditionDeadline(null);
            push.setOperatedAt(null);
            push.setOperatorId(null);
            candidatePushRepository.updateById(push);
            log.info("考生重新推送（重新开启）: candidateId={}", req.candidateId());
            operationLogService.log(push.getPushId(), "PUSH", null, "重新推送（补录轮次: " + push.getPushRound() + "）");
        } else {
            push = new CandidatePush();
            push.setSchoolId(schoolId);
            push.setCandidateId(req.candidateId());
            push.setCandidateName(req.candidateName());
            push.setNationality(req.nationality());
            push.setIdNumber(req.idNumber());
            push.setEmail(req.email());
            push.setTotalScore(req.totalScore() != null ? req.totalScore() : null);
            push.setSubjectScores(req.subjectScores());
            push.setIntention(req.intention());
            push.setStatus(CandidateStatus.PENDING.name());
            push.setPushRound(req.round() != null ? req.round() : 0);
            push.setPushedAt(Instant.now());
            push.setPushId(UUID.randomUUID());
            candidatePushRepository.insert(push);
            operationLogService.log(push.getPushId(), "PUSH", null,
                    "新推送，总分: " + (req.totalScore() != null ? req.totalScore() : "-") + "，轮次: " + push.getPushRound());
            log.info("新考生推送: candidateId={}, pushId={}", req.candidateId(), push.getPushId());

            // 站内通知：通知该校 SCHOOL_ADMIN
            notificationService.onCandidatePushed(
                    schoolId, push.getPushId(), push.getCandidateName(), push.getTotalScore());
        }

        return push;
    }

    /**
     * 处理考生确认录取（互斥逻辑）
     */
    @Transactional
    public void handleCandidateConfirmed(String candidateId, String confirmedSchoolId) {
        UUID confirmedSchoolUUID = UUID.fromString(confirmedSchoolId);

        // 查找该考生在所有院校的推送记录
        List<CandidatePush> allPushes = candidatePushRepository.findByCandidateId(candidateId);

        // 向已确认院校发送报到确认通知
        registrationPlatformClient.sendCheckinConfirm(
                confirmedSchoolUUID, candidateId,
                allPushes.stream()
                        .filter(p -> p.getSchoolId().equals(confirmedSchoolUUID))
                        .findFirst()
                        .map(CandidatePush::getCandidateName)
                        .orElse(""));

        for (CandidatePush push : allPushes) {
            if (push.getSchoolId().equals(confirmedSchoolUUID)) {
                // 已确认院校
                push.setStatus(CandidateStatus.CONFIRMED.name());
                push.setOperatedAt(Instant.now());
                candidatePushRepository.updateById(push);
                operationLogService.log(push.getPushId(), "CONFIRM", null, "考生确认录取");
                log.info("考生确认本校录取: pushId={}", push.getPushId());

                // 站内通知
                String majorName = majorRepository.findById(push.getAdmissionMajorId())
                        .map(Major::getMajorName).orElse("");
                notificationService.onCandidateConfirmed(
                        push.getSchoolId(), push.getPushId(), push.getCandidateName(), majorName);
            } else if (CandidateStatus.ADMITTED.name().equals(push.getStatus())) {
                // 其他发出录取通知的院校 → 失效 + 名额释放
                push.setStatus(CandidateStatus.INVALIDATED.name());
                push.setOperatedAt(Instant.now());
                candidatePushRepository.updateById(push);

                // 释放名额
                releaseQuota(push);
                operationLogService.log(push.getPushId(), "INVALIDATE", null, "其他院校已确认，本校录取失效");
                log.info("其他院校录取通知失效: pushId={}, schoolId={}", push.getPushId(), push.getSchoolId());
            }
        }
    }

    /**
     * 有条件录取到期处理
     */
    @Transactional
    public void handleConditionExpired(String candidateId, String schoolId) {
        UUID schoolUUID = UUID.fromString(schoolId);
        Optional<CandidatePush> opt = candidatePushRepository
                .findBySchoolAndCandidate(schoolUUID, candidateId);

        if (opt.isEmpty()) return;

        CandidatePush push = opt.get();
        if (!CandidateStatus.CONDITIONAL.name().equals(push.getStatus())) return;

        // 自动恢复到 PENDING
        push.setStatus(CandidateStatus.PENDING.name());
        push.setConditionDesc(null);
        push.setConditionDeadline(null);
        push.setOperatedAt(Instant.now());
        candidatePushRepository.updateById(push);

        // 释放预占名额
        releaseQuota(push);

        // 站内通知院校 + OP_ADMIN
        notificationService.onConditionExpired(push.getSchoolId(), push.getPushId(), push.getCandidateName());

        operationLogService.log(push.getPushId(), "CONDITION_EXPIRED", null, "条件到期，自动恢复为待处理");
        log.info("有条件录取到期自动恢复: pushId={}", push.getPushId());
    }

    /**
     * 全局搜索考生（按姓名、证件号、候选人ID）
     */
    public List<CandidatePush> searchCandidates(UUID schoolId, String keyword) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            q.eq(CandidatePush::getSchoolId, schoolId);
        }
        String kw = "%" + keyword + "%";
        q.and(w -> w
                .like(CandidatePush::getCandidateName, keyword)
                .or()
                .like(CandidatePush::getIdNumber, keyword)
                .or()
                .like(CandidatePush::getCandidateId, keyword))
                .orderByDesc(CandidatePush::getPushedAt)
                .last("LIMIT 20");
        return candidatePushRepository.selectList(q);
    }

    /**
     * 释放名额（admitted_count - 1 或 reserved_count - 1）
     */
    private void releaseQuota(CandidatePush push) {
        if (push.getAdmissionMajorId() == null) return;

        Optional<AdmissionQuota> opt = admissionQuotaRepository
                .findBySchoolMajorYear(push.getSchoolId(), push.getAdmissionMajorId(),
                        Calendar.getInstance().get(Calendar.YEAR));

        if (opt.isEmpty()) return;

        AdmissionQuota quota = opt.get();
        if (quota.getAdmittedCount() > 0) {
            quota.setAdmittedCount(quota.getAdmittedCount() - 1);
        } else if (quota.getReservedCount() > 0) {
            quota.setReservedCount(quota.getReservedCount() - 1);
        }
        admissionQuotaRepository.updateById(quota);
    }

    /**
     * 考生列表查询
     */
    public IPage<CandidatePush> queryCandidates(
            UUID schoolId,
            List<String> status,
            Float minScore, Float maxScore,
            String intentionKeyword,
            String nationality,
            Instant pushTimeStart, Instant pushTimeEnd,
            UUID majorId, Integer round,
            String sort, String order,
            int page, int pageSize) {

        Page<CandidatePush> p = new Page<>(page, pageSize);
        return candidatePushRepository.pageQuery(p, schoolId, status, minScore, maxScore,
                intentionKeyword, nationality, pushTimeStart, pushTimeEnd,
                majorId, round, sort, order);
    }

    /**
     * 考生列表导出（全量，不分页）
     */
    public List<CandidatePush> queryCandidatesForExport(
            UUID schoolId,
            List<String> status,
            Float minScore, Float maxScore,
            String intentionKeyword,
            String nationality,
            Instant pushTimeStart, Instant pushTimeEnd,
            UUID majorId, Integer round) {

        return candidatePushRepository.selectForExport(
                schoolId, status,
                minScore != null ? new java.math.BigDecimal(minScore.toString()) : null,
                maxScore != null ? new java.math.BigDecimal(maxScore.toString()) : null,
                intentionKeyword, nationality,
                pushTimeStart, pushTimeEnd, majorId, round);
    }

    /**
     * 获取考生详情
     */
    public Optional<CandidatePush> getById(UUID pushId) {
        return candidatePushRepository.findById(pushId);
    }
}
