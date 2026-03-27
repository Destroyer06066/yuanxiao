package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.CandidatePushRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CandidatePushRepository candidatePushRepository;

    /**
     * 返回 KPI 指标卡数据
     */
    public Map<String, Object> getKpis(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> baseQ = buildSchoolFilter(schoolId);

        long totalPushed = candidatePushRepository.selectCount(baseQ.clone());
        long admitted = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.ADMITTED));
        long confirmed = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.CONFIRMED));
        long checkedIn = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.CHECKED_IN));

        // 同比（简化：与去年相比）
        int thisYear = LocalDate.now().getYear();
        long totalLastYear = countByYear(schoolId, thisYear - 1);

        return Map.of(
                "totalPushed", totalPushed,
                "admitted", admitted,
                "confirmed", confirmed,
                "checkedIn", checkedIn,
                "totalLastYear", totalLastYear
        );
    }

    /**
     * 月度趋势：每个月的各状态数量
     */
    public List<Map<String, Object>> getMonthlyTrend(UUID schoolId, int year) {
        Instant startOfYear = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        q.between(CandidatePush::getOperatedAt, startOfYear, endOfYear);
        q.isNotNull(CandidatePush::getOperatedAt);

        List<CandidatePush> records = candidatePushRepository.selectList(q);

        // 按月份 + 状态分组
        Map<String, Map<String, Long>> grouped = new TreeMap<>();
        for (CandidatePush p : records) {
            String month = String.valueOf(
                    p.getOperatedAt().atZone(ZoneId.systemDefault()).getMonthValue());
            grouped.computeIfAbsent(month, k -> new HashMap<>())
                   .merge(p.getStatus(), 1L, Long::sum);
        }

        String[] months = {
                "1月","2月","3月","4月","5月","6月",
                "7月","8月","9月","10月","11月","12月"
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String key = String.valueOf(i + 1);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", months[i]);
            row.put("pending", grouped.getOrDefault(key, Map.of()).getOrDefault(CandidateStatus.PENDING.name(), 0L));
            row.put("admitted", grouped.getOrDefault(key, Map.of()).getOrDefault(CandidateStatus.ADMITTED.name(), 0L));
            row.put("confirmed", grouped.getOrDefault(key, Map.of()).getOrDefault(CandidateStatus.CONFIRMED.name(), 0L));
            row.put("checkedIn", grouped.getOrDefault(key, Map.of()).getOrDefault(CandidateStatus.CHECKED_IN.name(), 0L));
            row.put("rejected", grouped.getOrDefault(key, Map.of()).getOrDefault(CandidateStatus.REJECTED.name(), 0L));
            result.add(row);
        }
        return result;
    }

    /**
     * 状态分布：各状态考生数量
     */
    public List<Map<String, Object>> getStatusDistribution(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        Map<String, Long> counts = records.stream()
                .collect(Collectors.groupingBy(CandidatePush::getStatus, Collectors.counting()));

        String[][] statusConfig = {
                { CandidateStatus.PENDING.name(), "待处理", "#909399" },
                { CandidateStatus.ADMITTED.name(), "已录取", "#409eff" },
                { CandidateStatus.CONDITIONAL.name(), "有条件录取", "#e6a23c" },
                { CandidateStatus.CONFIRMED.name(), "已确认", "#67c23a" },
                { CandidateStatus.CHECKED_IN.name(), "已报到", "#25a861" },
                { CandidateStatus.REJECTED.name(), "已拒绝", "#f56c6c" },
                { CandidateStatus.INVALIDATED.name(), "已失效", "#c0c4cc" },
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] cfg : statusConfig) {
            result.add(Map.of(
                    "status", cfg[0],
                    "label", cfg[1],
                    "color", cfg[2],
                    "count", counts.getOrDefault(cfg[0], 0L)
            ));
        }
        return result;
    }

    /**
     * 专业录取排名 Top N
     */
    public List<Map<String, Object>> getMajorRanking(UUID schoolId, int limit) {
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        q.isNotNull(CandidatePush::getAdmissionMajorId);
        q.in(CandidatePush::getStatus,
                CandidateStatus.ADMITTED.name(),
                CandidateStatus.CONFIRMED.name(),
                CandidateStatus.CHECKED_IN.name());
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        // 按专业分组统计
        Map<UUID, Map<String, Object>> majorStats = new LinkedHashMap<>();
        for (CandidatePush p : records) {
            majorStats.computeIfAbsent(p.getAdmissionMajorId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("majorId", k);
                m.put("admitted", 0L);
                m.put("confirmed", 0L);
                m.put("checkedIn", 0L);
                return m;
            });
            String s = p.getStatus();
            Map<String, Object> stats = majorStats.get(p.getAdmissionMajorId());
            if (CandidateStatus.ADMITTED.name().equals(s)) {
                stats.put("admitted", ((Number) stats.get("admitted")).longValue() + 1);
            } else if (CandidateStatus.CONFIRMED.name().equals(s)) {
                stats.put("confirmed", ((Number) stats.get("confirmed")).longValue() + 1);
            } else if (CandidateStatus.CHECKED_IN.name().equals(s)) {
                stats.put("checkedIn", ((Number) stats.get("checkedIn")).longValue() + 1);
            }
        }

        return majorStats.values().stream()
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("confirmed") + (Long) b.get("checkedIn"),
                        (Long) a.get("confirmed") + (Long) a.get("checkedIn")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ========== 私有辅助 ==========

    private LambdaQueryWrapper<CandidatePush> buildSchoolFilter(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        return q;
    }

    private LambdaQueryWrapper<CandidatePush> buildStatusFilter(UUID schoolId, CandidateStatus status) {
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        q.eq(CandidatePush::getStatus, status.name());
        return q;
    }

    private long countByYear(UUID schoolId, int year) {
        Instant start = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.of(year, 12, 31, 23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        q.between(CandidatePush::getPushedAt, start, end);
        return candidatePushRepository.selectCount(q);
    }
}
