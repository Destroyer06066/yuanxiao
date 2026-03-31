package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID logId;

    @TableField("push_id")
    private UUID pushId;

    private String action;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("operator_name")
    private String operatorName;

    private String remark;

    @TableField("created_at")
    private Instant createdAt;
}
