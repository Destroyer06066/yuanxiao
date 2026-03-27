package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.entity.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

public interface AuditLogRepository extends BaseMapper<AuditLog> {

    IPage<AuditLog> selectPaged(Page<AuditLog> page,
                                @Param("operatorName") String operatorName,
                                @Param("action") String action,
                                @Param("startTime") Instant startTime,
                                @Param("endTime") Instant endTime);
}
