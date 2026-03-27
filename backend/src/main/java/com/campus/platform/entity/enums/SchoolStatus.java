package com.campus.platform.entity.enums;

public enum SchoolStatus {
    ACTIVE("启用"),
    INACTIVE("停用");

    private final String description;

    SchoolStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
