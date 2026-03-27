package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class PermissionModuleDTO {
    private String module;
    private String moduleLabel;
    private List<PermissionDTO> permissions;
}
