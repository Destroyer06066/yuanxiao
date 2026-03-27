package com.campus.platform.security;

import java.util.UUID;

/**
 * 基于 ThreadLocal 的安全上下文
 */
public class SecurityContext {

    private static final ThreadLocal<AccountPrincipal> CONTEXT = new ThreadLocal<>();

    public static void set(AccountPrincipal principal) {
        CONTEXT.set(principal);
    }

    public static AccountPrincipal get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static UUID getAccountId() {
        AccountPrincipal p = get();
        return p != null ? p.getAccountId() : null;
    }

    public static String getRole() {
        AccountPrincipal p = get();
        return p != null ? p.getRole() : null;
    }

    public static UUID getSchoolId() {
        AccountPrincipal p = get();
        return p != null ? p.getSchoolId() : null;
    }

    public static boolean isOpAdmin() {
        AccountPrincipal p = get();
        return p != null && p.isOpAdmin();
    }
}
