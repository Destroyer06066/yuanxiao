package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.VerificationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationLogRepository extends BaseMapper<VerificationLog> {

    List<VerificationLog> findBySchoolId(@Param("schoolId") UUID schoolId);

    List<VerificationLog> findByPushId(@Param("pushId") UUID pushId);

    Optional<VerificationLog> findById(@Param("verificationId") UUID verificationId);
}
