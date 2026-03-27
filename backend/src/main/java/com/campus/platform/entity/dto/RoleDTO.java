package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class RoleDTO {
    private UUID id;
    private String roleKey;
    private String name;
    private String description;
    private Boolean isPreset;
    private String presetKey;
    private String status;
    private List<PermissionDTO> permissions;
}
