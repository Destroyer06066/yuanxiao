package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("role_permission")
public class RolePermission {
    private UUID roleId;
    private UUID permissionId;
    private Boolean isExplicit;
}
