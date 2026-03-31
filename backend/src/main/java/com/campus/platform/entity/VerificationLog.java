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
@TableName("verification_log")
public class VerificationLog extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID verificationId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("push_id")
    private UUID pushId;

    @TableField("operator_id")
    private UUID operatorId;

    private String action;

    @TableField("certificate_no")
    private String certificateNo;

    private String result;

    private String note;
}
