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
@TableName("notification")
public class Notification extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID notificationId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("push_id")
    private UUID pushId;

    @TableField("recipient_id")
    private UUID recipientId;

    private String title;

    private String content;

    @TableField("is_read")
    private Boolean isRead;
}
