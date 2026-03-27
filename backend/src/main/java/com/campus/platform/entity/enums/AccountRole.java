package com.campus.platform.entity.enums;

public enum AccountRole {
    OP_ADMIN("运营管理员"),
    SCHOOL_ADMIN("院校管理员"),
    SCHOOL_STAFF("院校工作人员");

    private final String description;

    AccountRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canManageSchool() {
        return this == OP_ADMIN;
    }

    public boolean canManageStaff() {
        return this == OP_ADMIN || this == SCHOOL_ADMIN;
    }

    public boolean canConfigureSchool() {
        return this == OP_ADMIN || this == SCHOOL_ADMIN;
    }

    public boolean canOperateAdmission() {
        return this == SCHOOL_ADMIN || this == SCHOOL_STAFF;
    }

    public boolean isDataScoped() {
        return this == SCHOOL_ADMIN || this == SCHOOL_STAFF;
    }
}
