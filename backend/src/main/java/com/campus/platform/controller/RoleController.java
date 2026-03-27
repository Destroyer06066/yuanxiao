package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.dto.*;
import com.campus.platform.security.RequireRole;
import com.campus.platform.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取角色列表")
    @GetMapping
    @RequireRole("OP_ADMIN")
    public Result<List<RoleDTO>> list() {
        return Result.ok(roleService.listRoles());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public Result<RoleDTO> get(@PathVariable UUID id) {
        return Result.ok(roleService.getRole(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @RequireRole("OP_ADMIN")
    public Result<RoleDTO> create(@Valid @RequestBody CreateRoleRequest req) {
        return Result.ok(roleService.createRole(req));
    }

    @Operation(summary = "更新角色信息")
    @PutMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public Result<RoleDTO> update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateRoleRequest req) {
        return Result.ok(roleService.updateRole(id, req));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public Result<Void> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/{id}/permissions")
    @RequireRole("OP_ADMIN")
    public Result<RoleDTO> updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest req) {
        return Result.ok(roleService.updateRolePermissions(id, req));
    }
}
