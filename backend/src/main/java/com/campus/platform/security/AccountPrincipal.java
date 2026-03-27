package com.campus.platform.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AccountPrincipal {

    private UUID accountId;
    private String role;
    private UUID schoolId;
    private String realName;
    private String jti; // JWT ID，用于会话唯一性
    private List<String> permissions;

    public boolean isOpAdmin() {
        return "OP_ADMIN".equals(role);
    }

    public boolean isSchoolAdmin() {
        return "SCHOOL_ADMIN".equals(role);
    }

    public boolean isSchoolStaff() {
        return "SCHOOL_STAFF".equals(role);
    }
}
