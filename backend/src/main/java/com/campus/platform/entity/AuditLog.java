package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId
    private UUID auditId;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("operator_role")
    private String operatorRole;

    @TableField("school_id")
    private UUID schoolId;

    private String action;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private UUID targetId;

    @TableField("before_snapshot")
    private String beforeSnapshot;

    @TableField("after_snapshot")
    private String afterSnapshot;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("operated_at")
    private Instant operatedAt;
}
