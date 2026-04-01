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
@TableName("school_brochure")
public class SchoolBrochure extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID brochureId;

    @TableField("school_id")
    private UUID schoolId;

    private String title;

    private String content;

    private String status; // DRAFT / PUBLISHED

    @TableField("published_at")
    private Instant publishedAt;
}
