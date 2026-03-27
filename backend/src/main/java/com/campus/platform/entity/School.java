package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school")
public class School extends BaseEntity {

    @TableId(type = IdType.INPUT, value = "school_id")
    private UUID schoolId;

    @TableField("school_name")
    private String schoolName;

    @TableField("school_short_name")
    private String schoolShortName;

    private String province;

    @TableField("school_type")
    private String schoolType;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    private String website;

    private String remark;

    private String status;

    @TableField("created_by")
    private UUID createdBy;
}
