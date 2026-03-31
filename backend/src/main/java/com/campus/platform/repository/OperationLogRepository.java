package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface OperationLogRepository extends BaseMapper<OperationLog> {

    List<OperationLog> findByPushId(@Param("pushId") UUID pushId);

    List<OperationLog> findRecent(@Param("limit") int limit);
}
