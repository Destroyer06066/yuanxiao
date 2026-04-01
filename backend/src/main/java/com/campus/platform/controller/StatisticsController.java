package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "数据统计")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "KPI 指标卡")
    @GetMapping("/statistics/kpis")
    public Result<Map<String, Object>> getKpis(
            @RequestParam(required = false, defaultValue = "0") int year) {
        if (year == 0) year = java.time.LocalDate.now().getYear();
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getKpis(schoolId, year));
    }

    @Operation(summary = "可用年份列表")
    @GetMapping("/statistics/years")
    public Result<List<Integer>> getAvailableYears() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getAvailableYears(schoolId));
    }

    @Operation(summary = "录取趋势")
    @GetMapping("/statistics/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false, defaultValue = "0") int year,
            @RequestParam(required = false, defaultValue = "0") int month) {
        // year=0 表示当前年份
        if (year == 0) year = java.time.LocalDate.now().getYear();
        // schoolId=0/null 表示平台级（OP_ADMIN）
        UUID sid = SecurityContext.isOpAdmin()
                ? (schoolId != null && !schoolId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000")) ? schoolId : null)
                : SecurityContext.getSchoolId();
        // month=0 表示按年查询月度趋势，否则按月查询每日趋势
        if (month == 0) {
            return Result.ok(statisticsService.getMonthlyTrend(sid, year));
        } else {
            return Result.ok(statisticsService.getDailyTrend(sid, year, month));
        }
    }

    @Operation(summary = "考生状态分布")
    @GetMapping("/statistics/status-dist")
    public Result<List<Map<String, Object>>> getStatusDistribution() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getStatusDistribution(schoolId));
    }

    @Operation(summary = "专业录取排名")
    @GetMapping("/statistics/major-ranking")
    public Result<List<Map<String, Object>>> getMajorRanking(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getMajorRanking(schoolId, limit));
    }

    @Operation(summary = "各校录取进度排行（OP_ADMIN）")
    @GetMapping("/statistics/school-progress")
    public Result<List<Map<String, Object>>> getSchoolProgress() {
        return Result.ok(statisticsService.getSchoolProgress());
    }

    @Operation(summary = "异常提醒")
    @GetMapping("/statistics/alerts")
    public Result<Map<String, Object>> getAlerts() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getAlerts(schoolId));
    }

    @Operation(summary = "近期操作动态")
    @GetMapping("/statistics/recent-operations")
    public Result<List<Map<String, Object>>> getRecentOperations(
            @RequestParam(required = false, defaultValue = "20") int limit) {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getRecentOperations(schoolId, limit));
    }

    @Operation(summary = "名额使用概览（SCHOOL）")
    @GetMapping("/statistics/quota-usage")
    public Result<List<Map<String, Object>>> getQuotaUsage() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getQuotaUsage(schoolId));
    }

    @Operation(summary = "录取轮次分布")
    @GetMapping("/statistics/round-dist")
    public Result<Map<String, Object>> getRoundDistribution() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getRoundDistribution(schoolId));
    }

    @Operation(summary = "考生推送次数分布")
    @GetMapping("/statistics/push-count-dist")
    public Result<Map<String, Object>> getPushCountDistribution() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getPushCountDistribution(schoolId));
    }

    @Operation(summary = "院校录取区间分布（OP_ADMIN）")
    @GetMapping("/statistics/school-admission-ranges")
    @RequireRole({"OP_ADMIN"})
    public Result<List<Map<String, Object>>> getSchoolAdmissionRanges() {
        return Result.ok(statisticsService.getSchoolAdmissionRanges());
    }

    @Operation(summary = "性别分布")
    @GetMapping("/statistics/gender-dist")
    public Result<Map<String, Object>> getGenderDistribution() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getGenderDistribution(schoolId));
    }

    @Operation(summary = "年龄分布")
    @GetMapping("/statistics/age-dist")
    public Result<Map<String, Object>> getAgeDistribution() {
        UUID schoolId = SecurityContext.isOpAdmin() ? null : SecurityContext.getSchoolId();
        return Result.ok(statisticsService.getAgeDistribution(schoolId));
    }
}
