package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.SupplementInvitation;
import com.campus.platform.security.RequireRole;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.SupplementInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplement/invitations")
@RequiredArgsConstructor
@Tag(name = "补录邀请管理")
public class SupplementInvitationController {

    private final SupplementInvitationService supplementInvitationService;

    @Operation(summary = "发送补录邀请（模式二）")
    @PostMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<SupplementInvitation> sendInvitation(@RequestBody @Valid SendInvitationRequest req) {
        SupplementInvitation invitation = supplementInvitationService.sendInvitation(
                req.getPushId(),
                req.getMajorId(),
                req.getMessage(),
                SecurityContext.getAccountId()
        );
        return Result.ok(invitation);
    }

    @Operation(summary = "批量发送补录邀请（模式二）")
    @PostMapping("/batch")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF"})
    public Result<Integer> batchSendInvitation(@RequestBody @Valid BatchSendInvitationRequest req) {
        int count = supplementInvitationService.batchSendInvitation(
                req.getPushIds(),
                req.getMajorId(),
                req.getMessage(),
                SecurityContext.getAccountId()
        );
        return Result.ok(count);
    }

    @Operation(summary = "获取补录邀请列表")
    @GetMapping
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<List<SupplementInvitation>> listInvitations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer round,
            @RequestParam(required = false) String candidateKeyword) {
        List<SupplementInvitation> invitations = supplementInvitationService.listInvitations(
                SecurityContext.getSchoolId(),
                status,
                round,
                candidateKeyword
        );
        return Result.ok(invitations);
    }

    @Operation(summary = "获取补录邀请详情")
    @GetMapping("/{invitationId}")
    @RequireRole({"SCHOOL_ADMIN", "SCHOOL_STAFF", "OP_ADMIN"})
    public Result<SupplementInvitation> getInvitation(@PathVariable UUID invitationId) {
        SupplementInvitation invitation = supplementInvitationService.getById(invitationId);
        return Result.ok(invitation);
    }

    @Data
    public static class SendInvitationRequest {
        private UUID pushId;
        @Valid
        private java.util.UUID majorId;
        private String message;
    }

    @Data
    public static class BatchSendInvitationRequest {
        private List<UUID> pushIds;
        @Valid
        private UUID majorId;
        private String message;
    }
}
