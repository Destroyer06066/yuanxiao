package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admission_quota")
public class AdmissionQuota extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID quotaId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("major_id")
    private UUID majorId;

    private Integer year;

    @TableField("total_quota")
    private Integer totalQuota;

    @TableField("admitted_count")
    private Integer admittedCount;

    @TableField("reserved_count")
    private Integer reservedCount;

    @Version
    private Integer version;
}
