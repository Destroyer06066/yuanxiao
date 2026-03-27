package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.entity.CandidatePush;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidatePushRepository extends BaseMapper<CandidatePush> {

    Optional<CandidatePush> findById(@Param("pushId") UUID pushId);

    Optional<CandidatePush> findBySchoolAndCandidate(@Param("schoolId") UUID schoolId,
                                                      @Param("candidateId") String candidateId);

    List<CandidatePush> findByCandidateId(@Param("candidateId") String candidateId);

    IPage<CandidatePush> pageQuery(Page<CandidatePush> page,
                                   @Param("schoolId") UUID schoolId,
                                   @Param("statusList") List<String> statusList,
                                   @Param("minScore") Float minScore,
                                   @Param("maxScore") Float maxScore,
                                   @Param("intentionKeyword") String intentionKeyword,
                                   @Param("nationality") String nationality,
                                   @Param("pushTimeStart") Instant pushTimeStart,
                                   @Param("pushTimeEnd") Instant pushTimeEnd,
                                   @Param("majorId") UUID majorId,
                                   @Param("round") Integer round,
                                   @Param("sort") String sort,
                                   @Param("order") String order);

    List<CandidatePush> findExpiredConditionals(@Param("now") Instant now);

    /**
     * 导出查询：全量含 JOIN 字段
     */
    List<CandidatePush> selectForExport(@Param("schoolId") UUID schoolId,
                                       @Param("statusList") List<String> statusList,
                                       @Param("minScore") java.math.BigDecimal minScore,
                                       @Param("maxScore") java.math.BigDecimal maxScore,
                                       @Param("intentionKeyword") String intentionKeyword,
                                       @Param("nationality") String nationality,
                                       @Param("pushTimeStart") Instant pushTimeStart,
                                       @Param("pushTimeEnd") Instant pushTimeEnd,
                                       @Param("majorId") UUID majorId,
                                       @Param("round") Integer round);
}
