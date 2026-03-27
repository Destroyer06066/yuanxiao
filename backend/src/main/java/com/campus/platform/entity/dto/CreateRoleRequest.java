package com.campus.platform.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateRoleRequest {
    @NotBlank private String roleKey;
    @NotBlank private String name;
    private String description;
    @NotNull private String presetKey;
    private String status = "ACTIVE";
}
