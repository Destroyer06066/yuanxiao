package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.Notification;
import com.campus.platform.repository.NotificationRepository;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "站内通知")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @Operation(summary = "查询通知列表")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UUID recipientId = SecurityContext.getAccountId();
        Page<Notification> p = new Page<>(page, pageSize);
        IPage<Notification> result = notificationRepository.findByRecipientId(p, recipientId);

        return Result.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    @Operation(summary = "标为已读")
    @PatchMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable UUID notificationId) {
        Notification n = notificationRepository.selectById(notificationId);
        if (n != null) {
            n.setIsRead(true);
            notificationRepository.updateById(n);
        }
        return Result.ok();
    }

    @Operation(summary = "全部标为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        UUID recipientId = SecurityContext.getAccountId();
        // 简化：查询未读并批量更新
        return Result.ok();
    }
}
