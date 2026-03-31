package com.campus.platform.service;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.entity.Major;
import com.campus.platform.entity.ScoreLine;
import com.campus.platform.repository.MajorRepository;
import com.campus.platform.repository.ScoreLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreLineService {

    private final ScoreLineRepository scoreLineRepository;
    private final MajorRepository majorRepository;

    /**
     * 获取本校分数线列表，含 majorName
     */
    public List<Map<String, Object>> getBySchool(UUID schoolId) {
        List<ScoreLine> lines = scoreLineRepository.findBySchoolId(schoolId);
        // 收集所有 majorId
        List<UUID> majorIds = lines.stream()
                .map(ScoreLine::getMajorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> majorNameMap = majorIds.stream()
                .map(majorRepository::findById)
                .filter(opt -> opt.isPresent())
                .collect(Collectors.toMap(
                        opt -> opt.get().getMajorId(),
                        opt -> opt.get().getMajorName()
                ));

        return lines.stream().map(line -> {
            String majorName = line.getMajorId() != null
                    ? majorNameMap.getOrDefault(line.getMajorId(), "") : "";
            return Map.<String, Object>of(
                    "lineId", line.getLineId().toString(),
                    "majorId", line.getMajorId() != null ? line.getMajorId().toString() : "",
                    "majorName", majorName,
                    "year", line.getYear(),
                    "subject", line.getSubject(),
                    "minScore", line.getMinScore()
            );
        }).collect(Collectors.toList());
    }

    /**
     * 创建分数线
     */
    @Transactional
    public void create(UUID schoolId, ScoreLineRequest req) {
        // 校验不重复
        if (scoreLineRepository.existsBySchoolMajorYearSubject(
                schoolId, req.getMajorId(), req.getYear(), req.getSubject(), null)) {
            throw new BusinessException(ErrorCode.QUOTA_NOT_ENOUGH, "该分数线配置已存在");
        }
        ScoreLine line = new ScoreLine();
        line.setLineId(UUID.randomUUID());
        line.setSchoolId(schoolId);
        line.setMajorId(req.getMajorId());
        line.setYear(req.getYear());
        line.setSubject(req.getSubject());
        line.setMinScore(req.getMinScore());
        scoreLineRepository.insert(line);
        log.info("创建分数线: lineId={}, schoolId={}", line.getLineId(), schoolId);
    }

    /**
     * 更新分数线
     */
    @Transactional
    public void update(UUID lineId, ScoreLineRequest req) {
        ScoreLine line = scoreLineRepository.findById(lineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "分数线记录不存在"));
        if (scoreLineRepository.existsBySchoolMajorYearSubject(
                line.getSchoolId(), req.getMajorId(), req.getYear(), req.getSubject(), lineId)) {
            throw new BusinessException(ErrorCode.QUOTA_NOT_ENOUGH, "该分数线配置已存在");
        }
        line.setMajorId(req.getMajorId());
        line.setYear(req.getYear());
        line.setSubject(req.getSubject());
        line.setMinScore(req.getMinScore());
        scoreLineRepository.updateById(line);
        log.info("更新分数线: lineId={}", lineId);
    }

    /**
     * 删除分数线
     */
    @Transactional
    public void delete(UUID lineId) {
        ScoreLine line = scoreLineRepository.findById(lineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "分数线记录不存在"));
        scoreLineRepository.deleteById(lineId);
        log.info("删除分数线: lineId={}", lineId);
    }

    // ========== DTO ==========

    @lombok.Data
    public static class ScoreLineRequest {
        private UUID majorId;
        private Integer year;
        private String subject;
        private BigDecimal minScore;
    }
}
