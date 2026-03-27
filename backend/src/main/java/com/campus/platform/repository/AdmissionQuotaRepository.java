package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.AdmissionQuota;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionQuotaRepository extends BaseMapper<AdmissionQuota> {

    Optional<AdmissionQuota> findById(@Param("quotaId") UUID quotaId);

    List<AdmissionQuota> findBySchoolId(@Param("schoolId") UUID schoolId);

    Optional<AdmissionQuota> findBySchoolMajorYear(@Param("schoolId") UUID schoolId,
                                                     @Param("majorId") UUID majorId,
                                                     @Param("year") Integer year);
}
