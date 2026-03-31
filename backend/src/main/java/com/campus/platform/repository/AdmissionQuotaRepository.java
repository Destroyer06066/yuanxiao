package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.AdmissionQuota;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionQuotaRepository extends BaseMapper<AdmissionQuota> {

    @Select("SELECT * FROM admission_quota WHERE quota_id = #{quotaId} AND deleted = 0")
    Optional<AdmissionQuota> findById(@Param("quotaId") UUID quotaId);

    @Select("SELECT * FROM admission_quota WHERE school_id = #{schoolId} AND deleted = 0 ORDER BY created_at DESC")
    List<AdmissionQuota> findBySchoolId(@Param("schoolId") UUID schoolId);

    @Select("SELECT * FROM admission_quota WHERE school_id = #{schoolId} AND major_id = #{majorId} AND year = #{year} AND deleted = 0 LIMIT 1")
    Optional<AdmissionQuota> findBySchoolMajorYear(@Param("schoolId") UUID schoolId,
                                                     @Param("majorId") UUID majorId,
                                                     @Param("year") Integer year);

    default List<AdmissionQuota> findAll(@Param("schoolId") UUID schoolId,
                                          @Param("majorId") UUID majorId,
                                          @Param("year") Integer year) {
        LambdaQueryWrapper<AdmissionQuota> q = new LambdaQueryWrapper<>();
        if (schoolId != null) q.eq(AdmissionQuota::getSchoolId, schoolId);
        if (majorId != null) q.eq(AdmissionQuota::getMajorId, majorId);
        if (year != null) q.eq(AdmissionQuota::getYear, year);
        q.orderByDesc(AdmissionQuota::getCreatedAt);
        return selectList(q);
    }
}
