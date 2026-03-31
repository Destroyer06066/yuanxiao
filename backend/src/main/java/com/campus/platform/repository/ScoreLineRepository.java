package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.ScoreLine;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoreLineRepository extends BaseMapper<ScoreLine> {

    @Select("SELECT * FROM score_line WHERE school_id = #{schoolId} AND deleted = 0 ORDER BY year DESC, subject ASC")
    List<ScoreLine> findBySchoolId(@Param("schoolId") UUID schoolId);

    @Select("SELECT * FROM score_line WHERE line_id = #{lineId} AND deleted = 0")
    Optional<ScoreLine> findById(@Param("lineId") UUID lineId);

    @Select("SELECT EXISTS(SELECT 1 FROM score_line WHERE school_id=#{schoolId} AND major_id=#{majorId} AND year=#{year} AND subject=#{subject} AND line_id != #{excludeId})")
    boolean existsBySchoolMajorYearSubject(
            @Param("schoolId") UUID schoolId,
            @Param("majorId") UUID majorId,
            @Param("year") Integer year,
            @Param("subject") String subject,
            @Param("excludeId") UUID excludeId);
}
