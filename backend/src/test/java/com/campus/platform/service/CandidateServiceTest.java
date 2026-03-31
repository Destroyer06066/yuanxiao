package com.campus.platform.service;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.AdmissionQuota;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.integration.inbound.IntegrationController.CandidatePushRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock private CandidatePushRepository candidatePushRepository;
    @Mock private AdmissionQuotaRepository admissionQuotaRepository;
    @Mock private MajorRepository majorRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private RegistrationPlatformClient registrationPlatformClient;
    @Mock private OperationLogService operationLogService;
    @Mock private NotificationService notificationService;

    @InjectMocks private CandidateService candidateService;

    private UUID schoolId;
    private CandidatePushRequest validRequest;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        validRequest = new CandidatePushRequest(
                "C001",          // candidateId
                "张三",          // candidateName
                "中国",          // nationality
                "110101199001011234", // idNumber
                "zhang@example.com", // email
                BigDecimal.valueOf(580),        // totalScore
                Map.of("数学", BigDecimal.valueOf(120)), // subjectScores
                "计算机科学与技术", // intention
                schoolId.toString(), // schoolId
                1                   // round
        );
    }

    // ========== receivePush 测试 ==========

    @Nested
    @DisplayName("receivePush")
    class ReceivePushTests {

        @Test
        @DisplayName("新考生推送 → 插入记录，状态为 PENDING")
        void newCandidate_pushInserts() {
            when(candidatePushRepository.findBySchoolAndCandidate(any(), eq("C001")))
                    .thenReturn(Optional.empty());
            doAnswer(invocation -> { CandidatePush p = invocation.getArgument(0); p.setPushId(UUID.randomUUID()); return 1; })
                    .when(candidatePushRepository).insert(any(CandidatePush.class));

            CandidatePush result = candidateService.receivePush(validRequest);

            assertNotNull(result);
            assertEquals("C001", result.getCandidateId());
            assertEquals(CandidateStatus.PENDING.name(), result.getStatus());
            verify(candidatePushRepository).insert(any(CandidatePush.class));
            verify(candidatePushRepository, never()).updateById(any(CandidatePush.class));
        }

        @Test
        @DisplayName("ADMITTED 考生重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
        void admittedCandidate_duplicatePush_throws() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.ADMITTED.name());

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> candidateService.receivePush(validRequest));

            assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
        }

        @Test
        @DisplayName("REJECTED 考生重新推送 → 重启为 PENDING，更新记录")
        void rejectedCandidate_repush_reopens() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.REJECTED.name());
            existing.setTotalScore(BigDecimal.valueOf(500));

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            CandidatePush result = candidateService.receivePush(validRequest);

            assertEquals(CandidateStatus.PENDING.name(), result.getStatus());
            assertEquals(BigDecimal.valueOf(580), result.getTotalScore());
            verify(candidatePushRepository).updateById(existing);
            verify(candidatePushRepository, never()).insert(any(CandidatePush.class));
        }

        @Test
        @DisplayName("INVALIDATED 考生重新推送 → 重启为 PENDING")
        void invalidatedCandidate_repush_reopens() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.INVALIDATED.name());

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            CandidatePush result = candidateService.receivePush(validRequest);

            assertEquals(CandidateStatus.PENDING.name(), result.getStatus());
        }

        @Test
        @DisplayName("重新推送时清除录取相关字段")
        void repush_clearsAdmissionFields() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.REJECTED.name());
            existing.setAdmissionMajorId(UUID.randomUUID());
            existing.setConditionDesc("数学≥130");
            existing.setConditionDeadline(Instant.now().plusSeconds(86400));

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            CandidatePush result = candidateService.receivePush(validRequest);

            assertNull(result.getAdmissionMajorId());
            assertNull(result.getConditionDesc());
            assertNull(result.getConditionDeadline());
        }

        @Test
        @DisplayName("PENDING 状态重复推送 → 更新考生信息（允许重推）")
        void pendingCandidate_duplicatePush_updatesRecord() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.PENDING.name());
            existing.setTotalScore(new java.math.BigDecimal("500"));

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            CandidatePush result = candidateService.receivePush(validRequest);

            assertEquals(CandidateStatus.PENDING.name(), result.getStatus());
            assertEquals(new java.math.BigDecimal("580"), result.getTotalScore()); // updated from validRequest
            verify(candidatePushRepository).updateById(existing);
            verify(candidatePushRepository, never()).insert(any(CandidatePush.class));
        }

        @Test
        @DisplayName("CONDITIONAL 状态重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
        void conditionalCandidate_duplicatePush_throws() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.CONDITIONAL.name());

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> candidateService.receivePush(validRequest));

            assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
        }

        @Test
        @DisplayName("CONFIRMED 状态重复推送 → 抛出 INTEGRATION_PUSH_ERROR")
        void confirmedCandidate_duplicatePush_throws() {
            CandidatePush existing = new CandidatePush();
            existing.setPushId(UUID.randomUUID());
            existing.setSchoolId(schoolId);
            existing.setCandidateId("C001");
            existing.setStatus(CandidateStatus.CONFIRMED.name());

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> candidateService.receivePush(validRequest));

            assertEquals(ErrorCode.INTEGRATION_PUSH_ERROR, ex.getCode());
        }
    }

    // ========== handleCandidateConfirmed 测试 ==========

    @Nested
    @DisplayName("handleCandidateConfirmed")
    class HandleCandidateConfirmedTests {

        @Test
        @DisplayName("确认本校录取 → 其他院校已录取记录变为 INVALIDATED")
        void confirmsSchool_invalidatesOthers() {
            UUID schoolA = UUID.randomUUID();
            UUID schoolB = UUID.randomUUID();

            CandidatePush pushA = new CandidatePush();
            pushA.setPushId(UUID.randomUUID());
            pushA.setSchoolId(schoolA);
            pushA.setCandidateId("C001");
            pushA.setCandidateName("张三");
            pushA.setStatus(CandidateStatus.ADMITTED.name());
            pushA.setAdmissionMajorId(UUID.randomUUID());

            CandidatePush pushB = new CandidatePush();
            pushB.setPushId(UUID.randomUUID());
            pushB.setSchoolId(schoolB);
            pushB.setCandidateId("C001");
            pushB.setCandidateName("张三");
            pushB.setStatus(CandidateStatus.ADMITTED.name());
            pushB.setAdmissionMajorId(UUID.randomUUID());

            AdmissionQuota quotaA = new AdmissionQuota();
            quotaA.setQuotaId(UUID.randomUUID());
            quotaA.setSchoolId(schoolA);
            quotaA.setMajorId(pushA.getAdmissionMajorId());
            quotaA.setYear(LocalDate.now().getYear());
            quotaA.setAdmittedCount(10);
            quotaA.setReservedCount(0);

            when(candidatePushRepository.findByCandidateId("C001"))
                    .thenReturn(List.of(pushA, pushB));
            when(admissionQuotaRepository.findBySchoolMajorYear(any(UUID.class), any(UUID.class), any()))
                    .thenReturn(Optional.of(quotaA));
            doNothing().when(registrationPlatformClient)
                    .sendCheckinConfirm(any(), any(), any());
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            candidateService.handleCandidateConfirmed("C001", schoolA.toString());

            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    p.getSchoolId().equals(schoolA) &&
                    CandidateStatus.CONFIRMED.name().equals(p.getStatus())));
            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    p.getSchoolId().equals(schoolB) &&
                    CandidateStatus.INVALIDATED.name().equals(p.getStatus())));
            // 释放 schoolA 的名额
            assertEquals(9, quotaA.getAdmittedCount());
        }
    }

    // ========== handleConditionExpired 测试 ==========

    @Nested
    @DisplayName("handleConditionExpired")
    class HandleConditionExpiredTests {

        @Test
        @DisplayName("有条件录取到期 → 状态恢复 PENDING，释放 reserved_count")
        void conditionalExpired_resetsToPending() {
            UUID majorId = UUID.randomUUID();
            CandidatePush push = new CandidatePush();
            push.setPushId(UUID.randomUUID());
            push.setSchoolId(schoolId);
            push.setCandidateId("C001");
            push.setStatus(CandidateStatus.CONDITIONAL.name());
            push.setAdmissionMajorId(majorId);
            push.setConditionDesc("数学≥130");
            push.setConditionDeadline(Instant.now().minusSeconds(3600));

            AdmissionQuota quota = new AdmissionQuota();
            quota.setQuotaId(UUID.randomUUID());
            quota.setSchoolId(schoolId);
            quota.setMajorId(majorId);
            quota.setYear(LocalDate.now().getYear());
            quota.setAdmittedCount(0);
            quota.setReservedCount(3);

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(push));
            when(admissionQuotaRepository.findBySchoolMajorYear(schoolId, majorId, LocalDate.now().getYear()))
                    .thenReturn(Optional.of(quota));
            doAnswer(invocation -> 1).when(candidatePushRepository).updateById(any(CandidatePush.class));

            candidateService.handleConditionExpired("C001", schoolId.toString());

            verify(candidatePushRepository).updateById(argThat((CandidatePush p) ->
                    CandidateStatus.PENDING.name().equals(p.getStatus()) &&
                    p.getConditionDesc() == null));
            assertEquals(2, quota.getReservedCount()); // 3 - 1
        }

        @Test
        @DisplayName("考生不存在 → 无操作")
        void conditionExpired_candidateNotFound_noAction() {
            when(candidatePushRepository.findBySchoolAndCandidate(any(), any()))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() ->
                    candidateService.handleConditionExpired("C999", schoolId.toString()));

            verify(candidatePushRepository, never()).updateById(any(CandidatePush.class));
        }

        @Test
        @DisplayName("考生状态非 CONDITIONAL → 无操作")
        void conditionExpired_wrongStatus_noAction() {
            CandidatePush push = new CandidatePush();
            push.setPushId(UUID.randomUUID());
            push.setSchoolId(schoolId);
            push.setCandidateId("C001");
            push.setStatus(CandidateStatus.PENDING.name());

            when(candidatePushRepository.findBySchoolAndCandidate(schoolId, "C001"))
                    .thenReturn(Optional.of(push));

            assertDoesNotThrow(() ->
                    candidateService.handleConditionExpired("C001", schoolId.toString()));

            verify(candidatePushRepository, never()).updateById(any(CandidatePush.class));
        }
    }
}
