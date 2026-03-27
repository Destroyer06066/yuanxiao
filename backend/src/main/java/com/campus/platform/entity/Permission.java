package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("permission")
public class Permission {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String module;
    private String action;
    private String label;
    private String description;
}
