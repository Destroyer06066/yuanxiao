package com.campus.platform.controller;

import com.campus.platform.entity.dto.PermissionModuleDTO;
import com.campus.platform.security.RequireRole;
import com.campus.platform.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RoleService roleService;

    @Operation(summary = "获取权限树")
    @GetMapping
    @RequireRole("OP_ADMIN")
    public ResponseEntity<List<PermissionModuleDTO>> getPermissionTree() {
        return ResponseEntity.ok(roleService.getPermissionModules());
    }
}
