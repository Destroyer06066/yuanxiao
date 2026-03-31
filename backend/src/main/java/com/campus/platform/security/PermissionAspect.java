package com.campus.platform.security;

import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    // 处理方法级 RequireRole 注解
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        AccountPrincipal principal = SecurityContext.get();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
        }

        Set<String> allowed = Arrays.stream(requireRole.value())
                .collect(Collectors.toSet());

        if (!allowed.contains(principal.getRole())) {
            log.warn("权限不足: 账户 {} (role={}) 尝试访问需要 {} 的接口",
                    principal.getAccountId(), principal.getRole(), allowed);
            throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足，无权访问该功能");
        }
    }

    // 处理方法级 RequirePermission 注解
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        AccountPrincipal principal = SecurityContext.get();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
        }
        List<String> userPerms = principal.getPermissions();
        if (userPerms == null) userPerms = List.of();

        // OP_ADMIN 有 *
        if (userPerms.contains("*")) return;

        Set<String> required = Arrays.stream(requirePermission.value())
                .collect(Collectors.toSet());
        for (String r : required) {
            if (!userPerms.contains(r)) {
                log.warn("权限不足: 账户 {} 缺少权限 {}", principal.getAccountId(), r);
                throw new BusinessException(ErrorCode.FORBIDDEN, "权限不足，无权访问该功能");
            }
        }
    }
}
