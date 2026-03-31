package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_receive_log")
public class MaterialReceiveLog extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID receiveId;

    @TableField("push_id")
    private UUID pushId;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("receive_time")
    private Instant receiveTime;

    private String note;
}
