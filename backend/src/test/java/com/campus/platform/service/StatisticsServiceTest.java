package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import com.campus.platform.repository.MajorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatisticsServiceTest {

    @Mock private CandidatePushRepository candidatePushRepository;
    @Mock private MajorRepository majorRepository;

    @InjectMocks private StatisticsService statisticsService;

    private UUID schoolId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
    }

    // ========== getKpis 测试 ==========

    @Nested
    @DisplayName("getKpis")
    class GetKpisTests {

        @Test
        @DisplayName("返回各状态考生数量")
        void getKpis_returnsCounts() {
            when(candidatePushRepository.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(100L, 30L, 20L, 10L, 90L, 25L, 15L, 8L);

            Map<String, Object> result = statisticsService.getKpis(schoolId, 2026);

            assertNotNull(result);
            assertEquals(100L, result.get("totalPushed"));
            assertEquals(30L, result.get("admitted"));
            assertEquals(20L, result.get("confirmed"));
            assertEquals(10L, result.get("checkedIn"));
            assertEquals(90L, result.get("totalLastYear"));
            assertEquals(25L, result.get("admittedLastYear"));
            assertEquals(15L, result.get("confirmedLastYear"));
            assertEquals(8L, result.get("checkedInLastYear"));
        }

        @Test
        @DisplayName("schoolId=null 时统计全校数据")
        void getKpis_nullSchoolId_countsAll() {
            when(candidatePushRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(500L);

            Map<String, Object> result = statisticsService.getKpis(null, 2026);

            assertEquals(500L, result.get("totalPushed"));
            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(candidatePushRepository, atLeastOnce()).selectCount(captor.capture());
            // null schoolId 时调用 selectCount 不抛异常即通过
        }
    }

    // ========== getStatusDistribution 测试 ==========

    @Nested
    @DisplayName("getStatusDistribution")
    class GetStatusDistributionTests {

        @Test
        @DisplayName("返回所有状态及其数量，count=0 的也包含")
        void getStatusDistribution_includesAllStatuses() {
            CandidatePush pending = makePush(CandidateStatus.PENDING);
            CandidatePush admitted = makePush(CandidateStatus.ADMITTED);

            when(candidatePushRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(pending, pending, admitted));

            List<Map<String, Object>> result = statisticsService.getStatusDistribution(schoolId);

            assertNotNull(result);
            assertEquals(8, result.size()); // 8 个状态（含 MATERIAL_RECEIVED）

            // 找到 PENDING
            Map<String, Object> pendingRow = result.stream()
                    .filter(r -> "PENDING".equals(r.get("status")))
                    .findFirst().orElseThrow();
            assertEquals(2L, pendingRow.get("count"));

            // 找到 ADMITTED
            Map<String, Object> admittedRow = result.stream()
                    .filter(r -> "ADMITTED".equals(r.get("status")))
                    .findFirst().orElseThrow();
            assertEquals(1L, admittedRow.get("count"));

            // REJECTED count=0
            Map<String, Object> rejectedRow = result.stream()
                    .filter(r -> "REJECTED".equals(r.get("status")))
                    .findFirst().orElseThrow();
            assertEquals(0L, rejectedRow.get("count"));
        }
    }

    // ========== getMajorRanking 测试 ==========

    @Nested
    @DisplayName("getMajorRanking")
    class GetMajorRankingTests {

        @Test
        @DisplayName("按确认+报到总数降序排列，取 Top N")
        void getMajorRanking_sortedByConfirmed() {
            UUID majorA = UUID.randomUUID();
            UUID majorB = UUID.randomUUID();

            CandidatePush p1 = makePushWithMajor(majorA, CandidateStatus.CONFIRMED);
            CandidatePush p2 = makePushWithMajor(majorA, CandidateStatus.CONFIRMED);
            CandidatePush p3 = makePushWithMajor(majorA, CandidateStatus.CHECKED_IN);
            CandidatePush p4 = makePushWithMajor(majorB, CandidateStatus.ADMITTED);

            when(candidatePushRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(p1, p2, p3, p4));
            when(majorRepository.findById(any())).thenReturn(java.util.Optional.empty());

            List<Map<String, Object>> result = statisticsService.getMajorRanking(schoolId, 10);

            assertEquals(2, result.size());
            // majorA: 2 confirmed + 1 checkedIn = 3 → 排第一
            assertEquals(majorA, result.get(0).get("majorId"));
            assertEquals(3L, ((Number) result.get(0).get("confirmed")).longValue() + ((Number) result.get(0).get("checkedIn")).longValue());
            // majorB: 1 confirmed → 排第二
            assertEquals(majorB, result.get(1).get("majorId"));
        }

        @Test
        @DisplayName("limit=3 时最多返回3条")
        void getMajorRanking_respectsLimit() {
            UUID m1 = UUID.randomUUID(), m2 = UUID.randomUUID(),
                 m3 = UUID.randomUUID(), m4 = UUID.randomUUID();

            List<CandidatePush> pushes = List.of(
                    makePushWithMajor(m1, CandidateStatus.CONFIRMED),
                    makePushWithMajor(m2, CandidateStatus.CONFIRMED),
                    makePushWithMajor(m3, CandidateStatus.CONFIRMED),
                    makePushWithMajor(m4, CandidateStatus.CONFIRMED)
            );

            when(candidatePushRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(pushes);
            when(majorRepository.findById(any())).thenReturn(java.util.Optional.empty());

            List<Map<String, Object>> result = statisticsService.getMajorRanking(schoolId, 3);

            assertEquals(3, result.size());
        }
    }

    // ========== 辅助方法 ==========

    private CandidatePush makePush(CandidateStatus status) {
        CandidatePush p = new CandidatePush();
        p.setPushId(UUID.randomUUID());
        p.setSchoolId(schoolId);
        p.setCandidateId(UUID.randomUUID().toString());
        p.setStatus(status.name());
        p.setPushedAt(Instant.now());
        p.setOperatedAt(Instant.now());
        return p;
    }

    private CandidatePush makePushWithMajor(UUID majorId, CandidateStatus status) {
        CandidatePush p = makePush(status);
        p.setAdmissionMajorId(majorId);
        return p;
    }
}
