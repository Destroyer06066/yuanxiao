package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.SchoolConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolConfigRepository extends BaseMapper<SchoolConfig> {

    @Select("SELECT * FROM school_config WHERE school_id IS NULL AND config_key = #{configKey} AND deleted = 0 LIMIT 1")
    Optional<SchoolConfig> findGlobalConfig(@Param("configKey") String configKey);

    @Select("SELECT * FROM school_config WHERE school_id = #{schoolId} AND config_key = #{configKey} AND deleted = 0 LIMIT 1")
    Optional<SchoolConfig> findBySchoolAndKey(@Param("schoolId") UUID schoolId, @Param("configKey") String configKey);

    @Select("SELECT * FROM school_config WHERE school_id IS NULL AND deleted = 0 ORDER BY config_key")
    List<SchoolConfig> findAllGlobalConfigs();
}
