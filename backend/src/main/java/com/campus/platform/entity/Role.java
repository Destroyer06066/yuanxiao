package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends BaseEntity {
    @TableId
    private UUID id;

    private String roleKey;
    private String name;
    private String description;
    private Boolean isPreset;
    private String presetKey;
    private String status;

    @TableField(exist = false)
    private List<Permission> permissions = new ArrayList<>();
}
