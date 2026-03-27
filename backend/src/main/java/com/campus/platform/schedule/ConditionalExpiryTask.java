package com.campus.platform.schedule;

import com.campus.platform.entity.CandidatePush;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.service.CandidateService;
import com.campus.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 定时任务：有条件录取到期检查
 *
 * 每 10 分钟执行一次：
 * 1. 扫描即将到期（3天内）的 CONDITIONAL 考生，发送预警通知
 * 2. 扫描已到期（截止时间 <= 当前时间）的考生，自动恢复为 PENDING
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalExpiryTask {

    private final CandidatePushRepository candidatePushRepository;
    private final CandidateService candidateService;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @Transactional
    public void processExpiredConditionals() {
        Instant now = Instant.now();
        Instant threeDaysLater = now.plus(java.time.Duration.ofDays(3));
        log.info("[定时任务] 开始检查有条件录取到期: now={}", now);

        // 1. 即将到期提醒（3天内）
        List<CandidatePush> expiringSoon = candidatePushRepository.findExpiredConditionals(threeDaysLater);
        for (CandidatePush push : expiringSoon) {
            // 截止时间 > now 的才是"即将到期"，<= now 的是"已到期"
            if (push.getConditionDeadline() != null && push.getConditionDeadline().isAfter(now)) {
                String deadlineStr = LocalDate.ofInstant(push.getConditionDeadline(), ZoneId.systemDefault())
                        .format(DF);
                notificationService.onConditionExpiringSoon(
                        push.getSchoolId(), push.getPushId(), push.getCandidateName(), deadlineStr);
                log.debug("[定时任务] 即将到期提醒: pushId={}", push.getPushId());
            }
        }
        log.info("[定时任务] 即将到期提醒发送完毕: count={}", expiringSoon.size());

        // 2. 已到期自动处理
        List<CandidatePush> expired = candidatePushRepository.findExpiredConditionals(now);
        log.info("[定时任务] 找到 {} 条到期记录", expired.size());

        for (CandidatePush push : expired) {
            try {
                candidateService.handleConditionExpired(push.getCandidateId(), push.getSchoolId().toString());
            } catch (Exception e) {
                log.error("[定时任务] 处理条件到期失败: pushId={}", push.getPushId(), e);
            }
        }

        log.info("[定时任务] 有条件录取到期检查完成");
    }
}
