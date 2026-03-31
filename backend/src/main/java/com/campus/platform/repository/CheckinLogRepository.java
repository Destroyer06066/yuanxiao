package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.CheckinLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinLogRepository extends BaseMapper<CheckinLog> {

    @Select("SELECT * FROM checkin_log WHERE push_id IN (SELECT push_id FROM candidate_push WHERE school_id = #{schoolId} AND deleted = 0) ORDER BY checkin_time DESC")
    List<CheckinLog> findBySchoolId(@Param("schoolId") UUID schoolId);

    @Select("SELECT * FROM checkin_log WHERE push_id = #{pushId} LIMIT 1")
    Optional<CheckinLog> findByPushId(@Param("pushId") UUID pushId);
}
