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
@TableName("school_config")
public class SchoolConfig extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID configId;

    @TableField("school_id")
    private UUID schoolId;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;
}
