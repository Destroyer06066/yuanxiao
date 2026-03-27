package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PermissionDTO {
    private UUID id;
    private String module;
    private String action;
    private String label;
    private Boolean isExplicit;
    private Boolean isRestricted;
}
