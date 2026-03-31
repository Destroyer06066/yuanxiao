package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.MaterialReceiveLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

public interface MaterialReceiveLogRepository extends BaseMapper<MaterialReceiveLog> {

    @Select("SELECT * FROM material_receive_log WHERE push_id = #{pushId} LIMIT 1")
    Optional<MaterialReceiveLog> findByPushId(@Param("pushId") UUID pushId);
}
