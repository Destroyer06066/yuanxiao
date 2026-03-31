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
@TableName("checkin_log")
public class CheckinLog extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID checkinId;

    @TableField("push_id")
    private UUID pushId;

    @TableField("operator_id")
    private UUID operatorId;

    @TableField("checkin_time")
    private Instant checkinTime;

    private String note;
}
