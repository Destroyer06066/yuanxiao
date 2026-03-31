package com.campus.platform.security;

import com.campus.platform.common.exception.ErrorCode;
import com.campus.platform.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拦截器：处理类级别的 @RequireRole 注解（AOP无法可靠地处理类级注解）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        // 1. 检查方法级 RequireRole（由 AOP 处理，这里只兜底）
        RequireRole methodRole = AnnotationUtils.findAnnotation(hm.getMethod(), RequireRole.class);
        if (methodRole != null) return true; // AOP 会处理

        // 2. 检查类级 RequireRole（HandlerInterceptor 比 AOP 更可靠）
        RequireRole classRole = AnnotationUtils.findAnnotation(hm.getBeanType(), RequireRole.class);
        if (classRole == null) return true;

        // 执行角色检查
        return doCheckRole(classRole, response);
    }

    private boolean doCheckRole(RequireRole requireRole, HttpServletResponse response) throws IOException {
        AccountPrincipal principal = SecurityContext.get();
        if (principal == null) {
            writeResponse(response, 401, ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
            return false;
        }

        Set<String> allowed = Arrays.stream(requireRole.value())
                .collect(Collectors.toSet());

        if (!allowed.contains(principal.getRole())) {
            log.warn("权限不足: 账户 {} (role={}) 尝试访问需要 {} 的接口",
                    principal.getAccountId(), principal.getRole(), allowed);
            writeResponse(response, 403, ErrorCode.FORBIDDEN, "权限不足，无权访问该功能");
            return false;
        }
        return true;
    }

    private void writeResponse(HttpServletResponse response, int httpStatus, int errorCode, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Result<Void> result = Result.fail(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
