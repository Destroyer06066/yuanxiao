package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String roleKey;
    private String name;
    private String description;
    private Boolean isPreset;
    private String presetKey;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<Permission> permissions = new ArrayList<>();
}
