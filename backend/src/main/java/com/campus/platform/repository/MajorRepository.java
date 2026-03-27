package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Major;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MajorRepository extends BaseMapper<Major> {

    Optional<Major> findById(@Param("majorId") UUID majorId);

    List<Major> findBySchoolId(@Param("schoolId") UUID schoolId);

    boolean existsBySchoolIdAndName(@Param("schoolId") UUID schoolId, @Param("majorName") String majorName);
}
