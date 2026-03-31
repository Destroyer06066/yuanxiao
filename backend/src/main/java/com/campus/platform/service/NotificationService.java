package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.Account;
import com.campus.platform.entity.Notification;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.repository.AccountRepository;
import com.campus.platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 站内通知服务
 *
 * 在业务事件中调用，生成站内通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    /**
     * 创建站内通知
     */
    public void createNotification(UUID recipientId, UUID schoolId, UUID pushId,
                                   String title, String content) {
        Notification n = new Notification();
        n.setNotificationId(UUID.randomUUID());
        n.setRecipientId(recipientId);
        n.setSchoolId(schoolId);
        n.setPushId(pushId);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(false);
        n.setCreatedAt(Instant.now());
        notificationRepository.insert(n);
        log.debug("站内通知已创建: recipient={}, title={}", recipientId, title);
    }

    /**
     * 通知指定院校的所有 SCHOOL_ADMIN
     */
    public void notifySchoolAdmins(UUID schoolId, UUID pushId, String title, String content) {
        List<Account> admins = accountRepository.selectList(
                new LambdaQueryWrapper<Account>()
                        .eq(Account::getSchoolId, schoolId)
                        .eq(Account::getRole, AccountRole.SCHOOL_ADMIN.name())
                        .ne(Account::getStatus, "INACTIVE")
        );
        for (Account admin : admins) {
            createNotification(admin.getAccountId(), schoolId, pushId, title, content);
        }
        log.info("已通知院校 SCHOOL_ADMIN: schoolId={}, count={}", schoolId, admins.size());
    }

    /**
     * 通知所有 OP_ADMIN
     */
    public void notifyOpAdmins(UUID pushId, String title, String content) {
        List<Account> opAdmins = accountRepository.selectList(
                new LambdaQueryWrapper<Account>()
                        .eq(Account::getRole, AccountRole.OP_ADMIN.name())
                        .ne(Account::getStatus, "INACTIVE")
        );
        for (Account admin : opAdmins) {
            createNotification(admin.getAccountId(), null, pushId, title, content);
        }
        log.info("已通知 OP_ADMIN: count={}", opAdmins.size());
    }

    // ========== 业务事件触发点 ==========

    /**
     * 新考生推送
     */
    public void onCandidatePushed(UUID schoolId, UUID pushId,
                                  String candidateName, BigDecimal totalScore) {
        String title = "新考生推送";
        String content = String.format("考生 %s 已推送至贵校，总分 %s",
                candidateName, totalScore != null ? totalScore.toString() : "-");
        notifySchoolAdmins(schoolId, pushId, title, content);
    }

    /**
     * 考生确认录取
     */
    public void onCandidateConfirmed(UUID schoolId, UUID pushId,
                                     String candidateName, String majorName) {
        String title = "考生确认录取";
        String content = String.format("考生 %s 已确认录取贵校 %s", candidateName, majorName);
        notifySchoolAdmins(schoolId, pushId, title, content);
    }

    /**
     * 有条件录取即将到期（3天提醒）
     */
    public void onConditionExpiringSoon(UUID schoolId, UUID pushId,
                                        String candidateName, String deadline) {
        String title = "条件录取即将到期";
        String content = String.format("考生 %s 的条件录取将于 %s 到期，请及时处理", candidateName, deadline);
        notifySchoolAdmins(schoolId, pushId, title, content);
    }

    /**
     * 有条件录取已到期（已自动失效）
     */
    public void onConditionExpired(UUID schoolId, UUID pushId, String candidateName) {
        String title = "条件录取已失效";
        String content = String.format("考生 %s 的条件录取已到期，已自动恢复为待处理", candidateName);
        // 通知院校 + OP_ADMIN
        notifySchoolAdmins(schoolId, pushId, title, content);
        notifyOpAdmins(pushId, title, content);
    }

    /**
     * 名额使用超 90%
     */
    public void onQuotaOver90(UUID schoolId, String majorName, int used, int total) {
        int remaining = total - used;
        String title = "名额预警";
        String content = String.format("%s 名额使用已达 %d%%，剩余 %d 个",
                majorName, (int) ((double) used / total * 100), remaining);
        notifySchoolAdmins(schoolId, null, title, content);
    }
}
