package com.campus.platform.schedule;

import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.service.CandidateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionalExpiryTaskTest {

    @Mock private CandidatePushRepository candidatePushRepository;
    @Mock private CandidateService candidateService;

    @InjectMocks private ConditionalExpiryTask task;

    @Test
    @DisplayName("无到期记录 → 不调用 handleConditionExpired")
    void processExpiredConditionals_noExpired_noAction() {
        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of());

        task.processExpiredConditionals();

        verify(candidateService, never()).handleConditionExpired(anyString(), anyString());
    }

    @Test
    @DisplayName("有 2 条到期记录 → 各调用一次 handleConditionExpired")
    void processExpiredConditionals_twoExpired_callsHandlerTwice() {
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();

        CandidatePush p1 = makeConditionalPush("C001", schoolA);
        CandidatePush p2 = makeConditionalPush("C002", schoolB);

        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of(p1, p2));
        doNothing().when(candidateService).handleConditionExpired(anyString(), anyString());

        task.processExpiredConditionals();

        verify(candidateService).handleConditionExpired("C001", schoolA.toString());
        verify(candidateService).handleConditionExpired("C002", schoolB.toString());
    }

    @Test
    @DisplayName("单条记录处理抛出异常 → 捕获后继续处理剩余记录")
    void processExpiredConditionals_oneThrows_continuesOthers() {
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();

        CandidatePush p1 = makeConditionalPush("C001", schoolA);
        CandidatePush p2 = makeConditionalPush("C002", schoolB);

        when(candidatePushRepository.findExpiredConditionals(any(Instant.class)))
                .thenReturn(List.of(p1, p2));

        doThrow(new RuntimeException("DB 故障"))
                .when(candidateService).handleConditionExpired("C001", schoolA.toString());
        doNothing().when(candidateService)
                .handleConditionExpired("C002", schoolB.toString());

        assertDoesNotThrow(() -> task.processExpiredConditionals());

        verify(candidateService).handleConditionExpired("C002", schoolB.toString());
    }

    private CandidatePush makeConditionalPush(String candidateId, UUID schoolId) {
        CandidatePush push = new CandidatePush();
        push.setPushId(UUID.randomUUID());
        push.setCandidateId(candidateId);
        push.setSchoolId(schoolId);
        push.setStatus(CandidateStatus.CONDITIONAL.name());
        push.setConditionDeadline(Instant.now().minusSeconds(3600));
        return push;
    }
}
