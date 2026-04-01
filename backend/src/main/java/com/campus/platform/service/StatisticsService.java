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
    public Map<String, Object> getKpis(UUID schoolId, int year) {
        // 按年份过滤的基数查询
        long totalPushed = countByYear(schoolId, year);
        long admitted = countByStatusYear(schoolId, CandidateStatus.ADMITTED, year);
        long confirmed = countByStatusYear(schoolId, CandidateStatus.CONFIRMED, year);
        long checkedIn = countByStatusYear(schoolId, CandidateStatus.CHECKED_IN, year);

        // 同比（与去年相比）
        long totalLastYear = countByYear(schoolId, year - 1);
        long admittedLastYear = countByStatusYear(schoolId, CandidateStatus.ADMITTED, year - 1);
        long confirmedLastYear = countByStatusYear(schoolId, CandidateStatus.CONFIRMED, year - 1);
        long checkedInLastYear = countByStatusYear(schoolId, CandidateStatus.CHECKED_IN, year - 1);

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
     * 每日趋势：指定年月的每天录取人数
     */
    public List<Map<String, Object>> getDailyTrend(UUID schoolId, int year, int month) {
        Instant startOfMonth = LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfMonth = LocalDateTime.of(year, month, 1, 23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        // 计算月末最后一天
        endOfMonth = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        q.between(CandidatePush::getOperatedAt, startOfMonth, endOfMonth);
        q.isNotNull(CandidatePush::getOperatedAt);

        List<CandidatePush> records = candidatePushRepository.selectList(q);

        // 按日期 + 状态分组
        Map<Integer, Map<String, Long>> grouped = new TreeMap<>();
        for (CandidatePush p : records) {
            int day = p.getOperatedAt().atZone(ZoneId.systemDefault()).getDayOfMonth();
            grouped.computeIfAbsent(day, k -> new HashMap<>())
                   .merge(p.getStatus(), 1L, Long::sum);
        }

        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day + "日");
            row.put("admitted", grouped.getOrDefault(day, Map.of()).getOrDefault(CandidateStatus.ADMITTED.name(), 0L));
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

    /**
     * 录取轮次分布统计
     */
    public Map<String, Object> getRoundDistribution(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = buildSchoolFilter(schoolId);
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        Map<Integer, Long> counts = records.stream()
                .filter(p -> p.getPushRound() != null)
                .collect(Collectors.groupingBy(CandidatePush::getPushRound, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        counts.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    /**
     * 考生推送次数分布（按 candidate_id 去重统计被推送次数）
     * 1次、2次、3次、4次、5次、6次及以上
     */
    public Map<String, Object> getPushCountDistribution(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        // 按 candidate_id 分组，统计每个考生被推送的次数
        Map<String, Long> pushCounts = records.stream()
                .filter(p -> p.getCandidateId() != null)
                .collect(Collectors.groupingBy(CandidatePush::getCandidateId, Collectors.counting()));

        long once = pushCounts.values().stream().filter(c -> c == 1).count();
        long twice = pushCounts.values().stream().filter(c -> c == 2).count();
        long three = pushCounts.values().stream().filter(c -> c == 3).count();
        long four = pushCounts.values().stream().filter(c -> c == 4).count();
        long five = pushCounts.values().stream().filter(c -> c == 5).count();
        long sixPlus = pushCounts.values().stream().filter(c -> c >= 6).count();

        return Map.of(
                "once", once,
                "twice", twice,
                "three", three,
                "four", four,
                "five", five,
                "sixPlus", sixPlus,
                "total", once + twice + three + four + five + sixPlus
        );
    }

    /**
     * 性别分布统计
     */
    public Map<String, Object> getGenderDistribution(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        Map<String, Long> counts = records.stream()
                .filter(p -> p.getGender() != null)
                .collect(Collectors.groupingBy(CandidatePush::getGender, Collectors.counting()));

        long male = counts.getOrDefault("M", 0L);
        long female = counts.getOrDefault("F", 0L);
        long other = counts.getOrDefault("O", 0L);
        long unknown = records.stream()
                .filter(p -> p.getGender() == null)
                .map(CandidatePush::getCandidateId)
                .distinct()
                .count();

        return Map.of(
                "male", male,
                "female", female,
                "other", other,
                "unknown", unknown,
                "total", male + female + other + unknown
        );
    }

    /**
     * 年龄分布统计（按出生日期计算年龄）
     */
    public Map<String, Object> getAgeDistribution(UUID schoolId) {
        LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(CandidatePush::getSchoolId, schoolId);
        List<CandidatePush> records = candidatePushRepository.selectList(q);

        // 过滤有出生日期的记录
        List<CandidatePush> recordsWithBirthDate = records.stream()
                .filter(p -> p.getBirthDate() != null)
                .toList();

        int currentYear = LocalDate.now().getYear();

        // 按年龄段分组
        Map<String, Long> ageGroups = recordsWithBirthDate.stream()
                .collect(Collectors.groupingBy(p -> {
                    int birthYear = p.getBirthDate().getYear();
                    int age = currentYear - birthYear;
                    if (age < 18) return "17岁以下";
                    else if (age <= 25) return "18-25岁";
                    else if (age <= 30) return "26-30岁";
                    else if (age <= 35) return "31-35岁";
                    else if (age <= 40) return "36-40岁";
                    else return "41岁以上";
                }, Collectors.counting()));

        // 统计未知年龄（无出生日期）的独立考生数
        Set<String> candidateIdsWithBirthDate = recordsWithBirthDate.stream()
                .map(CandidatePush::getCandidateId)
                .collect(Collectors.toSet());
        long unknownAge = records.stream()
                .filter(p -> p.getBirthDate() == null && candidateIdsWithBirthDate.add(p.getCandidateId()))
                .count();

        return Map.of(
                "under18", ageGroups.getOrDefault("17岁以下", 0L),
                "age18to25", ageGroups.getOrDefault("18-25岁", 0L),
                "age26to30", ageGroups.getOrDefault("26-30岁", 0L),
                "age31to35", ageGroups.getOrDefault("31-35岁", 0L),
                "age36to40", ageGroups.getOrDefault("36-40岁", 0L),
                "over41", ageGroups.getOrDefault("41岁以上", 0L),
                "unknown", unknownAge,
                "total", records.stream().map(CandidatePush::getCandidateId).distinct().count()
        );
    }

    /**
     * 院校录取人数区间分布（OP_ADMIN 专用）
     * 区间：0-10, 11-30, 31-50, 51-100, 100+
     */
    public List<Map<String, Object>> getSchoolAdmissionRanges() {
        List<School> schools = schoolRepository.selectList(null);

        // 各院校录取人数（ADMITTED + CONDITIONAL）
        Map<String, Integer> schoolAdmitCounts = new HashMap<>();
        for (School school : schools) {
            LambdaQueryWrapper<CandidatePush> q = new LambdaQueryWrapper<>();
            q.eq(CandidatePush::getSchoolId, school.getSchoolId());
            q.in(CandidatePush::getStatus,
                    CandidateStatus.ADMITTED.name(),
                    CandidateStatus.CONDITIONAL.name());
            long count = candidatePushRepository.selectCount(q);
            schoolAdmitCounts.put(school.getSchoolId().toString(), (int) count);
        }

        // 按区间分组
        int range0to10 = 0, range11to30 = 0, range31to50 = 0, range51to100 = 0, range100plus = 0;
        int count0to10 = 0, count11to30 = 0, count31to50 = 0, count51to100 = 0, count100plus = 0;

        for (Map.Entry<String, Integer> e : schoolAdmitCounts.entrySet()) {
            int cnt = e.getValue();
            if (cnt <= 10) { count0to10++; range0to10 += cnt; }
            else if (cnt <= 30) { count11to30++; range11to30 += cnt; }
            else if (cnt <= 50) { count31to50++; range31to50 += cnt; }
            else if (cnt <= 100) { count51to100++; range51to100 += cnt; }
            else { count100plus++; range100plus += cnt; }
        }

        return List.of(
                Map.of("range", "0-10", "schoolCount", count0to10, "admissionCount", range0to10),
                Map.of("range", "11-30", "schoolCount", count11to30, "admissionCount", range11to30),
                Map.of("range", "31-50", "schoolCount", count31to50, "admissionCount", range31to50),
                Map.of("range", "51-100", "schoolCount", count51to100, "admissionCount", range51to100),
                Map.of("range", "100+", "schoolCount", count100plus, "admissionCount", range100plus)
        );
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
