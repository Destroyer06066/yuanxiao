package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("supplement_invitation")
public class SupplementInvitation {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID invitationId;

    private String candidateId;

    private UUID pushId;

    private UUID schoolId;

    private UUID invitationMajorId;

    private String status;

    private Integer supplementRound;

    private String message;

    private Instant sentAt;

    private Instant expiresAt;

    private Instant respondedAt;

    private Instant createdAt;

    private Instant updatedAt;

    // JOIN 扩展字段（非数据库列）
    @TableField(exist = false) private String schoolName;
    @TableField(exist = false) private String majorName;
    @TableField(exist = false) private String candidateName;
}
