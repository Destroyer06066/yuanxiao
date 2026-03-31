package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.*;
import com.campus.platform.entity.enums.CandidateStatus;
import com.campus.platform.repository.*;
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
    private final SchoolRepository schoolRepository;
    private final OperationLogRepository operationLogRepository;
    private final AdmissionQuotaRepository admissionQuotaRepository;
    private final MajorRepository majorRepository;

    /**
     * 返回有数据的年份列表
     */
    public List<Integer> getAvailableYears(UUID schoolId) {
        List<Integer> years = candidatePushRepository.findDistinctYears(schoolId);
        int currentYear = java.time.LocalDate.now().getYear();
        // 确保当前年份在列表中
        if (years.isEmpty() || !years.contains(currentYear)) {
            years = new java.util.ArrayList<>(years);
            if (!years.contains(currentYear)) {
                years.add(0, currentYear);
            }
        }
        return years;
    }

    /**
     * 返回 KPI 指标卡数据
     */
    public Map<String, Object> getKpis(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> baseQ = buildSchoolFilter(schoolId);

        long totalPushed = candidatePushRepository.selectCount(baseQ.clone());
        long admitted = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.ADMITTED));
        long confirmed = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.CONFIRMED));
        long checkedIn = candidatePushRepository.selectCount(buildStatusFilter(schoolId, CandidateStatus.CHECKED_IN));

        // 同比（与去年相比）
        int thisYear = LocalDate.now().getYear();
        long totalLastYear = countByYear(schoolId, thisYear - 1);
        long admittedLastYear = countByStatusYear(schoolId, CandidateStatus.ADMITTED, thisYear - 1);
        long confirmedLastYear = countByStatusYear(schoolId, CandidateStatus.CONFIRMED, thisYear - 1);
        long checkedInLastYear = countByStatusYear(schoolId, CandidateStatus.CHECKED_IN, thisYear - 1);

        return Map.of(
                "totalPushed", totalPushed,
                "admitted", admitted,
                "confirmed", confirmed,
                "checkedIn", checkedIn,
                "totalLastYear", totalLastYear,
                "admittedLastYear", admittedLastYear,
                "confirmedLastYear", confirmedLastYear,
                "checkedInLastYear", checkedInLastYear
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
                { CandidateStatus.CONDITIONAL.name(), "有条件录取中", "#e6a23c" },
                { CandidateStatus.ADMITTED.name(), "已录取（待确认）", "#409eff" },
                { CandidateStatus.CONFIRMED.name(), "已确认", "#67c23a" },
                { CandidateStatus.MATERIAL_RECEIVED.name(), "材料已收", "#9c27b0" },
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
     * 各校录取进度排行（OP_ADMIN）
     */
    public List<Map<String, Object>> getSchoolProgress() {
        List<School> schools = schoolRepository.selectList(null);

        List<Map<String, Object>> result = new ArrayList<>();
        for (School school : schools) {
            UUID schoolId = school.getSchoolId();
            long pushed = candidatePushRepository.selectCount(
                    new LambdaQueryWrapper<CandidatePush>().eq(CandidatePush::getSchoolId, schoolId));
            long admitted = candidatePushRepository.selectCount(
                    new LambdaQueryWrapper<CandidatePush>()
                            .eq(CandidatePush::getSchoolId, schoolId)
                            .in(CandidatePush::getStatus,
                                    CandidateStatus.ADMITTED.name(),
                                    CandidateStatus.CONFIRMED.name(),
                                    CandidateStatus.CHECKED_IN.name(),
                                    CandidateStatus.MATERIAL_RECEIVED.name()));
            long confirmed = candidatePushRepository.selectCount(
                    new LambdaQueryWrapper<CandidatePush>()
                            .eq(CandidatePush::getSchoolId, schoolId)
                            .in(CandidatePush::getStatus,
                                    CandidateStatus.CONFIRMED.name(),
                                    CandidateStatus.CHECKED_IN.name(),
                                    CandidateStatus.MATERIAL_RECEIVED.name()));
            long checkedIn = candidatePushRepository.selectCount(
                    new LambdaQueryWrapper<CandidatePush>()
                            .eq(CandidatePush::getSchoolId, schoolId)
                            .eq(CandidatePush::getStatus, CandidateStatus.CHECKED_IN.name()));

            double rate = pushed > 0 ? (double) admitted / pushed * 100 : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("schoolId", schoolId);
            row.put("schoolName", school.getSchoolName());
            row.put("pushed", pushed);
            row.put("admitted", admitted);
            row.put("confirmed", confirmed);
            row.put("checkedIn", checkedIn);
            row.put("admissionRate", Math.round(rate * 10) / 10.0);
            result.add(row);
        }

        // 按录取率排序
        result.sort((a, b) -> Double.compare(
                (Double) b.get("admissionRate"),
                (Double) a.get("admissionRate")));
        return result;
    }

    /**
     * 异常提醒（OP_ADMIN / SCHOOL）
     */
    public Map<String, Object> getAlerts(UUID schoolId) {
        Instant now = Instant.now();
        Instant threeDaysLater = now.plus(java.time.Duration.ofDays(3));

        // 1. 有条件录取即将到期（3天内）
        LambdaQueryWrapper<CandidatePush> condQ = new LambdaQueryWrapper<>();
        if (schoolId != null) condQ.eq(CandidatePush::getSchoolId, schoolId);
        condQ.eq(CandidatePush::getStatus, CandidateStatus.CONDITIONAL.name());
        condQ.isNotNull(CandidatePush::getConditionDeadline);
        condQ.le(CandidatePush::getConditionDeadline, threeDaysLater);
        long expiringSoon = candidatePushRepository.selectCount(condQ);

        // 2. 名额使用超 90% 的专业
        LambdaQueryWrapper<AdmissionQuota> quotaQ = new LambdaQueryWrapper<>();
        if (schoolId != null) quotaQ.eq(AdmissionQuota::getSchoolId, schoolId);
        List<AdmissionQuota> quotas = admissionQuotaRepository.selectList(quotaQ);
        long over90Count = quotas.stream()
                .filter(q -> {
                    int total = q.getTotalQuota() != null ? q.getTotalQuota() : 0;
                    int admitted = q.getAdmittedCount() != null ? q.getAdmittedCount() : 0;
                    int reserved = q.getReservedCount() != null ? q.getReservedCount() : 0;
                    return total > 0 && (double) (admitted + reserved) / total >= 0.9;
                })
                .count();

        // 3. 今日新推送
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        LambdaQueryWrapper<CandidatePush> todayQ = new LambdaQueryWrapper<>();
        if (schoolId != null) todayQ.eq(CandidatePush::getSchoolId, schoolId);
        todayQ.ge(CandidatePush::getPushedAt, startOfDay);
        long todayPushed = candidatePushRepository.selectCount(todayQ);

        return Map.of(
                "conditionExpiringSoon", expiringSoon,
                "quotaOver90", over90Count,
                "todayPushed", todayPushed
        );
    }

    /**
     * 近期操作动态
     */
    public List<Map<String, Object>> getRecentOperations(UUID schoolId, int limit) {
        List<OperationLog> logs = operationLogRepository.findRecent(limit);

        // 收集 pushIds
        List<UUID> pushIds = logs.stream().map(OperationLog::getPushId).filter(Objects::nonNull).distinct().toList();

        // 批量查询考生信息和院校信息
        final Map<UUID, CandidatePush> pushMapFinal;
        Map<UUID, CandidatePush> tmpMap = new HashMap<>();
        if (!pushIds.isEmpty()) {
            List<CandidatePush> pushes = candidatePushRepository.selectList(
                    new LambdaQueryWrapper<CandidatePush>()
                            .in(CandidatePush::getPushId, pushIds)
                            .select(CandidatePush::getPushId, CandidatePush::getSchoolId,
                                    CandidatePush::getCandidateName));
            tmpMap.putAll(pushes.stream().collect(Collectors.toMap(CandidatePush::getPushId, p -> p)));
        }
        pushMapFinal = Collections.unmodifiableMap(tmpMap);

        final Map<UUID, String> schoolNameMapFinal;
        boolean isOpAdmin = schoolId == null;
        if (isOpAdmin) {
            // OP_ADMIN 才需要查学校名
            Map<UUID, String> tmp = schoolRepository.selectList(null).stream()
                    .collect(Collectors.toMap(School::getSchoolId, School::getSchoolName));
            schoolNameMapFinal = Collections.unmodifiableMap(tmp);
        } else {
            schoolNameMapFinal = Collections.emptyMap();
        }

        return logs.stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("logId", log.getLogId());
            item.put("action", log.getAction());
            item.put("operatorName", log.getOperatorName());
            item.put("remark", log.getRemark());
            item.put("createdAt", log.getCreatedAt());

            CandidatePush push = pushMapFinal.get(log.getPushId());
            if (push != null) {
                item.put("candidateName", push.getCandidateName());
                item.put("schoolId", push.getSchoolId());
                item.put("schoolName",
                        isOpAdmin ? schoolNameMapFinal.getOrDefault(push.getSchoolId(), "") : null);
            }
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 名额使用概览（SCHOOL Dashboard）
     */
    public List<Map<String, Object>> getQuotaUsage(UUID schoolId) {
        LambdaQueryWrapper<AdmissionQuota> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(AdmissionQuota::getSchoolId, schoolId);
        List<AdmissionQuota> quotas = admissionQuotaRepository.selectList(q);

        // 批量查专业名
        List<UUID> majorIds = quotas.stream()
                .map(AdmissionQuota::getMajorId).filter(Objects::nonNull).distinct().toList();
        final Map<UUID, String> majorNameMapFinal;
        if (!majorIds.isEmpty()) {
            Map<UUID, String> tmp = majorRepository.selectList(
                    new LambdaQueryWrapper<Major>().in(Major::getMajorId, majorIds)
                            .select(Major::getMajorId, Major::getMajorName))
                    .stream().collect(Collectors.toMap(Major::getMajorId, Major::getMajorName));
            majorNameMapFinal = Collections.unmodifiableMap(tmp);
        } else {
            majorNameMapFinal = Collections.emptyMap();
        }

        return quotas.stream().map(qt -> {
            int total = qt.getTotalQuota() != null ? qt.getTotalQuota() : 0;
            int admitted = qt.getAdmittedCount() != null ? qt.getAdmittedCount() : 0;
            int reserved = qt.getReservedCount() != null ? qt.getReservedCount() : 0;
            int used = admitted + reserved;
            double usageRate = total > 0 ? (double) used / total * 100 : 0;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("quotaId", qt.getQuotaId());
            m.put("majorId", qt.getMajorId());
            m.put("majorName", majorNameMapFinal.getOrDefault(qt.getMajorId(), "未知专业"));
            m.put("totalQuota", total);
            m.put("used", used);
            m.put("remaining", total - used);
            m.put("usageRate", Math.round(usageRate * 10) / 10.0);
            return m;
        }).collect(Collectors.toList());
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
                // 补充专业名称
                majorRepository.findById(k).ifPresent(major -> m.put("majorName", major.getMajorName()));
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

    private long countByStatusYear(UUID schoolId, CandidateStatus status, int year) {
        Instant start = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.of(year, 12, 31, 23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        q.eq(CandidatePush::getStatus, status.name());
        q.between(CandidatePush::getPushedAt, start, end);
        return candidatePushRepository.selectCount(q);
    }
}
