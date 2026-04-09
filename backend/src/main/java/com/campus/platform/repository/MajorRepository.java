package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Major;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MajorRepository extends BaseMapper<Major> {

    @Select("SELECT * FROM major WHERE major_id = #{majorId} AND deleted = 0")
    Optional<Major> findById(@Param("majorId") UUID majorId);

    @Select("SELECT * FROM major WHERE school_id = #{schoolId} AND deleted = 0 ORDER BY created_at DESC")
    List<Major> findBySchoolId(@Param("schoolId") UUID schoolId);

    @Select("SELECT * FROM major WHERE school_id = #{schoolId} AND status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<Major> findBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") String status);

    @Select("SELECT COUNT(*) > 0 FROM major WHERE school_id = #{schoolId} AND major_name = #{majorName} AND deleted = 0")
    boolean existsBySchoolIdAndName(@Param("schoolId") UUID schoolId, @Param("majorName") String majorName);

    @Select("SELECT * FROM major WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<Major> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM major WHERE deleted = 0 ORDER BY created_at DESC")
    List<Major> findAll();
}
