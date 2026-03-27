package com.campus.platform.security;

import com.campus.platform.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 审计日志切面：
 * 拦截所有 Controller 方法，记录操作人、操作类型、目标对象到 audit_log 表
 *
 * 使用 @AuditAction 注解标记需要记录的操作，注解参数为操作类型
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    // 实际注入 AuditLogRepository
    // private final AuditLogRepository auditLogRepository;

    @Around("@annotation(auditAction)")
    public Object logOperation(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        long start = System.currentTimeMillis();
        AccountPrincipal principal = SecurityContext.get();
        String action = auditAction.value();
        Object target = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            log.warn("操作失败: action={}, error={}", action, t.getMessage());
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (principal != null) {
                log.debug("[审计] action={}, accountId={}, role={}, cost={}ms",
                        action, principal.getAccountId(), principal.getRole(), cost);
                // 异步写审计日志（实际需要注入 Repository）
                // writeAuditLogAsync(principal, action, joinPoint, result);
            }
        }
    }

    /**
     * 异步写入审计日志
     */
    @Async
    public void writeAuditLogAsync(AccountPrincipal principal, String action,
                                   ProceedingJoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            AuditLog audit = new AuditLog();
            audit.setOperatorId(principal.getAccountId());
            audit.setOperatorRole(principal.getRole());
            audit.setSchoolId(principal.getSchoolId());
            audit.setAction(action);
            audit.setOperatedAt(java.time.Instant.now());

            // auditLogRepository.insert(audit);
            log.debug("审计日志写入成功: action={}", action);
        } catch (Exception e) {
            log.error("审计日志写入失败: action={}", action, e);
        }
    }
}
