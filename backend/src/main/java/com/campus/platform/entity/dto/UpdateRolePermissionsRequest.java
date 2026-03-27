package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateRolePermissionsRequest {
    private List<UUID> permissionIds;
}
