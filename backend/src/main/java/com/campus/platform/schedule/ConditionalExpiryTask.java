package com.campus.platform.schedule;

import com.campus.platform.entity.CandidatePush;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 定时任务：有条件录取到期检查
 *
 * 每 10 分钟执行一次，扫描所有 CONDITIONAL 状态且条件截止时间 <= 当前时间的考生，
 * 自动将其状态恢复为 PENDING，并释放预占名额。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalExpiryTask {

    private final CandidatePushRepository candidatePushRepository;
    private final CandidateService candidateService;

    /**
     * 每 10 分钟执行一次
     * 固定延迟：上次执行完成后延迟 10 分钟再执行
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @Transactional
    public void processExpiredConditionals() {
        Instant now = Instant.now();
        log.info("[定时任务] 开始检查有条件录取到期: now={}", now);

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
