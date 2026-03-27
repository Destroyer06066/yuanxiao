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
@TableName("account")
public class Account extends BaseEntity {

    /** 使用 @TableId(value="account_id") 指定列名，避免 @TableField 与 @TableId 冲突 */
    @TableId(type = IdType.ASSIGN_UUID, value = "account_id")
    private UUID accountId;

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    private String role;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("real_name")
    private String realName;

    private String phone;

    private String status;

    @TableField("failed_login_count")
    private Integer failedLoginCount;

    @TableField("locked_until")
    private Instant lockedUntil;

    @TableField("last_login_at")
    private Instant lastLoginAt;

    @TableField("must_change_password")
    private Boolean mustChangePassword;

    @TableField("created_by")
    private UUID createdBy;
}
