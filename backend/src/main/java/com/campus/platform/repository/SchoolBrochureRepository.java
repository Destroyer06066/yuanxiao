package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.SchoolBrochure;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

public interface SchoolBrochureRepository extends BaseMapper<SchoolBrochure> {

    @Select("SELECT * FROM school_brochure WHERE school_id = #{schoolId} AND deleted = 0 LIMIT 1")
    Optional<SchoolBrochure> findBySchoolId(@Param("schoolId") UUID schoolId);
}
