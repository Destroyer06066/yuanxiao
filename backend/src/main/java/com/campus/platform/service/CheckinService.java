package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.CheckinLog;
import com.campus.platform.entity.MaterialReceiveLog;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.CheckinLogRepository;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.repository.MaterialReceiveLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinLogRepository checkinLogRepository;
    private final MaterialReceiveLogRepository materialReceiveLogRepository;
    private final CandidatePushRepository candidatePushRepository;
    private final MajorRepository majorRepository;
    private final OperationLogService operationLogService;

    /**
     * 获取报到列表（已录取/已确认/已收件/已报到的考生）
     */
    public List<Map<String, Object>> getCheckinList(UUID schoolId, String status, Boolean materialReceived) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        q.eq(CandidatePush::getSchoolId, schoolId)
                .in(CandidatePush::getStatus, List.of(
                        CandidateStatus.ADMITTED.name(),
                        CandidateStatus.CONFIRMED.name(),
                        CandidateStatus.MATERIAL_RECEIVED.name(),
                        CandidateStatus.CHECKED_IN.name()))
                .orderByDesc(CandidatePush::getOperatedAt);

        if (status != null && !status.isBlank()) {
            q.eq(CandidatePush::getStatus, status);
        }

        List<CandidatePush> pushes = candidatePushRepository.selectList(q);

        // 后端支持 materialReceived 筛选（按 operatedAt 非空判断）
        if (materialReceived != null) {
            if (materialReceived) {
                pushes = pushes.stream()
                        .filter(p -> p.getOperatedAt() != null
                                && (CandidateStatus.MATERIAL_RECEIVED.name().equals(p.getStatus())
                                    || CandidateStatus.CHECKED_IN.name().equals(p.getStatus())))
                        .collect(Collectors.toList());
            } else {
                pushes = pushes.stream()
                        .filter(p -> p.getOperatedAt() == null
                                || CandidateStatus.ADMITTED.name().equals(p.getStatus())
                                || CandidateStatus.CONFIRMED.name().equals(p.getStatus()))
                        .collect(Collectors.toList());
            }
        }

        return pushes.stream().map(p -> {
            String majorName = "";
            if (p.getMajorId() != null) {
                majorName = majorRepository.findById(p.getMajorId())
                        .map(m -> m.getMajorName()).orElse("");
            }
            String statusDesc = CandidateStatus.valueOf(p.getStatus()).getDescription();
            return Map.<String, Object>of(
                    "pushId", p.getPushId().toString(),
                    "candidateId", p.getCandidateId() != null ? p.getCandidateId() : "",
                    "candidateName", p.getCandidateName() != null ? p.getCandidateName() : "",
                    "majorId", p.getMajorId() != null ? p.getMajorId().toString() : "",
                    "majorName", majorName,
                    "status", p.getStatus(),
                    "statusDesc", statusDesc,
                    "receiveTime", (CandidateStatus.MATERIAL_RECEIVED.name().equals(p.getStatus())
                            || CandidateStatus.CHECKED_IN.name().equals(p.getStatus()))
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : "",
                    "checkinTime", CandidateStatus.CHECKED_IN.name().equals(p.getStatus())
                            ? (p.getOperatedAt() != null ? p.getOperatedAt().toString() : "") : ""
            );
        }).collect(Collectors.toList());
    }

    /**
     * 登记材料收件
     */
    @Transactional
    public void receiveMaterial(UUID schoolId, String pushId, String operatorId, String note) {
        UUID pushUuid = UUID.fromString(pushId);
        CandidatePush push = candidatePushRepository.findById(pushUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.CONFIRMED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许登记收件");
        }

        // 写入收件日志
        MaterialReceiveLog receiveLog = new MaterialReceiveLog();
        receiveLog.setReceiveId(UUID.randomUUID());
        receiveLog.setPushId(pushUuid);
        receiveLog.setOperatorId(operatorId != null ? UUID.fromString(operatorId) : null);
        receiveLog.setReceiveTime(Instant.now());
        receiveLog.setNote(note);
        materialReceiveLogRepository.insert(receiveLog);

        // 更新考生状态
        push.setStatus(CandidateStatus.MATERIAL_RECEIVED.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(operatorId != null ? UUID.fromString(operatorId) : null);
        candidatePushRepository.updateById(push);

        operationLogService.log(pushUuid,
                "MATERIAL_RECEIVE",
                operatorId != null ? UUID.fromString(operatorId) : null,
                "登记材料收件" + (note != null && !note.isBlank() ? "，备注: " + note : ""));

        log.info("登记材料收件: pushId={}, operatorId={}", pushId, operatorId);
    }

    /**
     * 确认报到
     */
    @Transactional
    public void doCheckin(UUID schoolId, String pushId, String operatorId, String note) {
        UUID pushUuid = UUID.fromString(pushId);
        CandidatePush push = candidatePushRepository.findById(pushUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND, "考生记录不存在"));

        if (!CandidateStatus.MATERIAL_RECEIVED.name().equals(push.getStatus())) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "请先登记收件");
        }

        // 写入报到日志
        CheckinLog checkinLog = new CheckinLog();
        checkinLog.setCheckinId(UUID.randomUUID());
        checkinLog.setPushId(pushUuid);
        checkinLog.setOperatorId(operatorId != null ? UUID.fromString(operatorId) : null);
        checkinLog.setCheckinTime(Instant.now());
        checkinLog.setNote(note);
        checkinLogRepository.insert(checkinLog);

        // 更新考生状态
        push.setStatus(CandidateStatus.CHECKED_IN.name());
        push.setOperatedAt(Instant.now());
        push.setOperatorId(operatorId != null ? UUID.fromString(operatorId) : null);
        candidatePushRepository.updateById(push);

        operationLogService.log(pushUuid,
                "CHECKIN",
                operatorId != null ? UUID.fromString(operatorId) : null,
                "确认报到" + (note != null && !note.isBlank() ? "，备注: " + note : ""));

        log.info("确认报到: pushId={}, operatorId={}", pushId, operatorId);
    }
}
