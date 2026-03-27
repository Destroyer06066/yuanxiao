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
@TableName("major")
public class Major extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private UUID majorId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("major_name")
    private String majorName;

    @TableField("degree_level")
    private String degreeLevel;

    private String status;
}
