package com.campus.platform.entity.dto;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String name;
    private String description;
    private String status;
}
