package com.campus.platform.service;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.AdmissionQuota;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.integration.outbound.RegistrationPlatformClient;
import com.campus.platform.repository.AdmissionQuotaRepository;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock private CandidatePushRepository candidatePushRepository;
    @Mock private AdmissionQuotaRepository admissionQuotaRepository;
    @Mock private MajorRepository majorRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private RegistrationPlatformClient registrationPlatformClient;
    @Mock private RedisService redisService;
    @Mock private OperationLogService operationLogService;
    @Mock private NotificationService notificationService;

    @InjectMocks private AdmissionService admissionService;

    private UUID pushId;
    private UUID majorId;
    private UUID schoolId;
    private UUID operatorId;
    private CandidatePush pendingPush;
    private AdmissionQuota quota;

    @BeforeEach
    void setUp() {
        pushId = UUID.randomUUID();
        majorId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        operatorId = UUID.randomUUID();

        pendingPush = new CandidatePush();
        pendingPush.setPushId(pushId);
        pendingPush.setSchoolId(schoolId);
        pendingPush.setCandidateId("C001");
        pendingPush.setCandidateName("张三");
        pendingPush.setStatus(CandidateStatus.PENDING.name());
        pendingPush.setTotalScore(BigDecimal.valueOf(580));
        pendingPush.setMajorId(majorId);
        pendingPush.setAdmissionMajorId(majorId);

        quota = new AdmissionQuota();
        quota.setQuotaId(UUID.randomUUID());
        quota.setSchoolId(schoolId);
        quota.setMajorId(majorId);
        quota.setYear(LocalDate.now().getYear());
        quota.setTotalQuota(50);
        quota.setAdmittedCount(10);
        quota.setReservedCount(5);
        quota.setVersion(0);
    }

    // ========== 直接录取测试 ==========

    @Nested
    @DisplayName("directAdmission")
    class DirectAdmissionTests {

        @BeforeEach
        void setUpLock() {
            lenient().when(redisService.tryLock(any(), any())).thenReturn(true);
            lenient().doNothing().when(redisService).unlock(any());
        }

        @Test
        @DisplayName("名额充足 → 录取成功，admitted_count +1")
        void directAdmission_success() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));
            doNothing().when(registrationPlatformClient)
                    .sendAdmissionNotify(any(), any(), any(), any(), any());

            admissionService.directAdmission(pushId, majorId, "表现优异", operatorId);

            assertEquals(11, quota.getAdmittedCount()); // 10 + 1
            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.ADMITTED.name().equals(p.getStatus()) &&
                    majorId.equals(p.getAdmissionMajorId())));
            verify(registrationPlatformClient).sendAdmissionNotify(eq(schoolId),
                    eq("C001"), eq("张三"), anyString(), eq("表现优异"));
        }

        @Test
        @DisplayName("名额已满 → 抛出 QUOTA_NOT_ENOUGH")
        void directAdmission_noQuota() {
            quota.setAdmittedCount(50);
            quota.setReservedCount(0);
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.directAdmission(pushId, majorId, null, operatorId));

            assertEquals(ErrorCode.QUOTA_NOT_ENOUGH, ex.getCode());
        }

        @Test
        @DisplayName("考生不存在 → 抛出 CANDIDATE_NOT_FOUND")
        void directAdmission_candidateNotFound() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.directAdmission(pushId, majorId, null, operatorId));

            assertEquals(ErrorCode.CANDIDATE_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("考生状态非 PENDING → 抛出 STATUS_TRANSITION_INVALID")
        void directAdmission_wrongStatus() {
            pendingPush.setStatus(CandidateStatus.ADMITTED.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.directAdmission(pushId, majorId, null, operatorId));

            assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
        }

        @Test
        @DisplayName("名额配置不存在 → 抛出 QUOTA_NOT_FOUND")
        void directAdmission_quotaNotFound() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.directAdmission(pushId, majorId, null, operatorId));

            assertEquals(ErrorCode.QUOTA_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("分布式锁获取失败 → 抛出 DUPLICATE_ADMISSION")
        void directAdmission_lockFailed() {
            when(redisService.tryLock(any(), any())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.directAdmission(pushId, majorId, null, operatorId));

            assertEquals(ErrorCode.DUPLICATE_ADMISSION, ex.getCode());
        }
    }

    // ========== 有条件录取测试 ==========

    @Nested
    @DisplayName("conditionalAdmission")
    class ConditionalAdmissionTests {

        @BeforeEach
        void setUpLock() {
            lenient().when(redisService.tryLock(any(), any())).thenReturn(true);
            lenient().doNothing().when(redisService).unlock(any());
        }

        @Test
        @DisplayName("有条件录取成功 → reserved_count +1")
        void conditionalAdmission_success() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));
            doNothing().when(registrationPlatformClient)
                    .sendConditionalNotify(any(), any(), any(), any(), any(), any());

            LocalDate deadline = LocalDate.now().plusDays(30);
            admissionService.conditionalAdmission(pushId, majorId, "数学单科≥120", deadline, operatorId);

            assertEquals(6, quota.getReservedCount()); // 5 + 1
            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.CONDITIONAL.name().equals(p.getStatus()) &&
                    "数学单科≥120".equals(p.getConditionDesc())));
            verify(registrationPlatformClient).sendConditionalNotify(
                    eq(schoolId), eq("C001"), eq("张三"), anyString(),
                    eq("数学单科≥120"), eq(deadline.toString()));
        }

        @Test
        @DisplayName("有条件录取：名额配置不存在 → 抛出 QUOTA_NOT_FOUND")
        void conditionalAdmission_quotaNotFound() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.conditionalAdmission(pushId, majorId,
                            "数学≥120", LocalDate.now().plusDays(30), operatorId));

            assertEquals(ErrorCode.QUOTA_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("有条件录取：锁获取失败 → 抛出 DUPLICATE_ADMISSION")
        void conditionalAdmission_lockFailed() {
            when(redisService.tryLock(any(), any())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.conditionalAdmission(pushId, majorId,
                            "数学≥120", LocalDate.now().plusDays(30), operatorId));

            assertEquals(ErrorCode.DUPLICATE_ADMISSION, ex.getCode());
        }

        @Test
        @DisplayName("有条件录取：考生状态非 PENDING → 抛出 STATUS_TRANSITION_INVALID")
        void conditionalAdmission_wrongStatus() {
            pendingPush.setStatus(CandidateStatus.ADMITTED.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.conditionalAdmission(pushId, majorId,
                            "数学≥120", LocalDate.now().plusDays(30), operatorId));

            assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
        }
    }

    // ========== 终裁录取测试 ==========

    @Nested
    @DisplayName("finalAdmission")
    class FinalAdmissionTests {

        @Test
        @DisplayName("终裁成功 → reserved_count -1，admitted_count +1")
        void finalAdmission_success() {
            pendingPush.setStatus(CandidateStatus.CONDITIONAL.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));
            doNothing().when(registrationPlatformClient)
                    .sendAdmissionNotify(any(), any(), any(), any(), any());

            admissionService.finalAdmission(pushId, operatorId);

            assertEquals(4, quota.getReservedCount()); // 5 - 1
            assertEquals(11, quota.getAdmittedCount()); // 10 + 1
            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.ADMITTED.name().equals(p.getStatus())));
        }

        @Test
        @DisplayName("终裁：考生不存在 → 抛出 CANDIDATE_NOT_FOUND")
        void finalAdmission_candidateNotFound() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.finalAdmission(pushId, operatorId));

            assertEquals(ErrorCode.CANDIDATE_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("终裁：考生状态非 CONDITIONAL → 抛出 STATUS_TRANSITION_INVALID")
        void finalAdmission_wrongStatus() {
            pendingPush.setStatus(CandidateStatus.PENDING.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.finalAdmission(pushId, operatorId));

            assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
        }
    }

    // ========== 撤销录取测试 ==========

    @Nested
    @DisplayName("revokeAdmission")
    class RevokeAdmissionTests {

        @Test
        @DisplayName("撤销已录取 → 释放 admitted_count")
        void revokeAdmitted_releasesAdmittedCount() {
            pendingPush.setStatus(CandidateStatus.ADMITTED.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            admissionService.revokeAdmission(pushId, operatorId);

            assertEquals(9, quota.getAdmittedCount()); // 10 - 1
            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.PENDING.name().equals(p.getStatus())));
        }

        @Test
        @DisplayName("撤销有条件录取 → 释放 reserved_count")
        void revokeConditional_releasesReservedCount() {
            pendingPush.setStatus(CandidateStatus.CONDITIONAL.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            admissionService.revokeAdmission(pushId, operatorId);

            assertEquals(4, quota.getReservedCount()); // 5 - 1
        }

        @Test
        @DisplayName("撤销 PENDING 状态 → 抛出 STATUS_TRANSITION_INVALID")
        void revokePending_throws() {
            pendingPush.setStatus(CandidateStatus.PENDING.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.revokeAdmission(pushId, operatorId));

            assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
        }

        @Test
        @DisplayName("撤销：考生不存在 → 抛出 CANDIDATE_NOT_FOUND")
        void revokeAdmission_candidateNotFound() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.revokeAdmission(pushId, operatorId));

            assertEquals(ErrorCode.CANDIDATE_NOT_FOUND, ex.getCode());
        }
    }

    // ========== 拒绝录取测试 ==========

    @Nested
    @DisplayName("rejectAdmission")
    class RejectAdmissionTests {

        @Test
        @DisplayName("拒绝 PENDING 考生 → 成功")
        void rejectPending_success() {
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));
            doNothing().when(registrationPlatformClient)
                    .sendRejectNotify(any(), any(), any(), any());

            admissionService.rejectAdmission(pushId, "分数不达标", operatorId);

            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.REJECTED.name().equals(p.getStatus())));
        }

        @Test
        @DisplayName("拒绝有条件录取 → 先释放 reserved_count")
        void rejectConditional_releasesQuota() {
            pendingPush.setStatus(CandidateStatus.CONDITIONAL.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));
            doNothing().when(registrationPlatformClient)
                    .sendRejectNotify(any(), any(), any(), any());

            admissionService.rejectAdmission(pushId, "条件未满足", operatorId);

            assertEquals(4, quota.getReservedCount()); // 5 - 1
        }

        @Test
        @DisplayName("拒绝 ADMITTED 考生 → 抛出 STATUS_TRANSITION_INVALID")
        void rejectAdmitted_throws() {
            pendingPush.setStatus(CandidateStatus.ADMITTED.name());
            when(candidatePushRepository.findById(pushId)).thenReturn(Optional.of(pendingPush));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> admissionService.rejectAdmission(pushId, "测试原因", operatorId));

            assertEquals(ErrorCode.STATUS_TRANSITION_INVALID, ex.getCode());
        }
    }
}
