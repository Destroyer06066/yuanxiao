# 角色权限管理模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的角色权限管理模块，支持角色的增删改查和按钮级权限分配

**Architecture:** 后端新增 role/permission 表 + Service 层，前端重构 RoleManage.vue 为运行时数据驱动，权限数据通过 Pinia store 管理并替换现有的静态配置

**Tech Stack:** Spring Boot 3.4 (Java 21) + MyBatis-Plus + Vue 3 + Pinia + Element Plus 2.7

---

## 文件总览

```
Backend:
- Create: backend/src/main/resources/db/migration/V4__role_permission_system.sql
- Create: backend/src/main/java/com/campus/platform/entity/Role.java
- Create: backend/src/main/java/com/campus/platform/entity/Permission.java
- Create: backend/src/main/java/com/campus/platform/entity/RolePermission.java
- Create: backend/src/main/java/com/campus/platform/entity/dto/RoleDTO.java
- Create: backend/src/main/java/com/campus/platform/entity/dto/PermissionDTO.java
- Create: backend/src/main/java/com/campus/platform/repository/RoleRepository.java
- Create: backend/src/main/java/com/campus/platform/repository/PermissionRepository.java
- Create: backend/src/main/java/com/campus/platform/repository/RolePermissionRepository.java
- Create: backend/src/main/java/com/campus/platform/service/RoleService.java
- Modify: backend/src/main/java/com/campus/platform/controller/RoleController.java
- Create: backend/src/main/java/com/campus/platform/controller/PermissionController.java
- Create: backend/src/main/java/com/campus/platform/security/RequirePermission.java
- Modify: backend/src/main/java/com/campus/platform/security/PermissionAspect.java
- Modify: backend/src/main/java/com/campus/platform/security/AccountPrincipal.java
- Modify: backend/src/main/java/com/campus/platform/security/JwtTokenProvider.java

Frontend:
- Modify: frontend/src/api/role.ts
- Create: frontend/src/api/permission.ts
- Create: frontend/src/stores/permission.ts
- Modify: frontend/src/composables/usePermission.ts
- Modify: frontend/src/views/admin/RoleManage.vue
```

---

## Task 1: 数据库迁移

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__role_permission_system.sql`

- [ ] **Step 1: 创建 Flyway 迁移脚本**

```sql
-- V4__role_permission_system.sql

-- 角色表
CREATE TABLE role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_key        VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(50) NOT NULL,
    description     VARCHAR(200),
    is_preset       BOOLEAN NOT NULL DEFAULT false,
    preset_key      VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_role_preset_key ON role (preset_key);
CREATE INDEX idx_role_status ON role (status);

-- 权限表
CREATE TABLE permission (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module          VARCHAR(50) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    label           VARCHAR(100) NOT NULL,
    description     VARCHAR(200),
    UNIQUE(module, action)
);

CREATE INDEX idx_permission_module ON permission (module);

-- 角色-权限关联表
CREATE TABLE role_permission (
    role_id         UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    is_explicit     BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permission_role ON role_permission (role_id);
CREATE INDEX idx_role_permission_perm ON role_permission (permission_id);

-- ===== 预设角色数据 =====
-- 运营管理员
INSERT INTO role (role_key, name, description, is_preset, preset_key, status)
VALUES ('OP_ADMIN', '运营管理员', '拥有系统全部权限，可管理所有院校、账号和数据。', true, NULL, 'ACTIVE');

-- 院校管理员
INSERT INTO role (role_key, name, description, is_preset, preset_key, status)
VALUES ('SCHOOL_ADMIN', '院校管理员', '管理本校的账号、专业、名额、成绩核验和报到确认等业务操作。', true, NULL, 'ACTIVE');

-- 院校工作人员
INSERT INTO role (role_key, name, description, is_preset, preset_key, status)
VALUES ('SCHOOL_STAFF', '院校工作人员', '查看本校数据，进行材料收件登记等辅助性操作。', true, NULL, 'ACTIVE');

-- ===== 权限数据 =====
INSERT INTO permission (module, action, label, description) VALUES
('account', 'read', '查看账号', NULL),
('account', 'create', '新增账号', NULL),
('account', 'edit', '编辑账号', NULL),
('account', 'disable', '禁用/启用账号', NULL),
('major', 'read', '查看专业', NULL),
('major', 'create', '新增专业', NULL),
('major', 'edit', '编辑专业', NULL),
('major', 'disable', '停用/启用专业', NULL),
('quota', 'read', '查看名额', NULL),
('quota', 'create', '新增名额', NULL),
('quota', 'edit', '编辑名额', NULL),
('verification', 'read', '查看核验', NULL),
('verification', 'create', '提交核验', NULL),
('checkin', 'read', '查看报到', NULL),
('checkin', 'material', '材料收件登记', NULL),
('checkin', 'confirm', '确认报到', NULL),
('report', 'read', '查看报表', NULL);

-- ===== OP_ADMIN: 全部权限 =====
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.role_key = 'OP_ADMIN';

-- ===== SCHOOL_ADMIN: 全部权限（is_explicit=false 表示从预设继承的）=====
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.role_key = 'SCHOOL_ADMIN';

-- ===== SCHOOL_STAFF: 只读权限 =====
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.role_key = 'SCHOOL_STAFF'
  AND p.action = 'read';
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/resources/db/migration/V4__role_permission_system.sql
git commit -m "feat(role): V4 migration - role/permission tables"
```

---

## Task 2: 后端实体类

**Files:**
- Create: `backend/src/main/java/com/campus/platform/entity/Role.java`
- Create: `backend/src/main/java/com/campus/platform/entity/Permission.java`
- Create: `backend/src/main/java/com/campus/platform/entity/RolePermission.java`
- Create: `backend/src/main/java/com/campus/platform/entity/dto/RoleDTO.java`
- Create: `backend/src/main/java/com/campus/platform/entity/dto/PermissionDTO.java`

- [ ] **Step 1: 创建 Role 实体**

```java
package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String roleKey;
    private String name;
    private String description;
    private Boolean isPreset;
    private String presetKey;
    private String status; // ACTIVE / INACTIVE

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<Permission> permissions = new ArrayList<>();
}
```

- [ ] **Step 2: 创建 Permission 实体**

```java
package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("permission")
public class Permission {
    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String module;
    private String action;
    private String label;
    private String description;
}
```

- [ ] **Step 3: 创建 RolePermission 实体（关联表）**

```java
package com.campus.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.UUID;

@Data
@TableName("role_permission")
public class RolePermission {
    private UUID roleId;
    private UUID permissionId;
    private Boolean isExplicit; // true=显式分配，false=从预设继承
}
```

- [ ] **Step 4: 创建 RoleDTO**

```java
package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class RoleDTO {
    private UUID id;
    private String roleKey;
    private String name;
    private String description;
    private Boolean isPreset;
    private String presetKey;
    private String status;
    private List<PermissionDTO> permissions; // 合并后的完整权限列表（含 isExplicit）
}
```

- [ ] **Step 5: 创建 PermissionDTO**

```java
package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PermissionDTO {
    private UUID id;
    private String module;
    private String action;
    private String label;
    private Boolean isExplicit; // 当前角色是否拥有该权限（computed）
    private Boolean isRestricted; // 是否不可分配给自定义角色
}
```

- [ ] **Step 6: 创建 PermissionModuleDTO**

```java
package com.campus.platform.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class PermissionModuleDTO {
    private String module;
    private String moduleLabel; // 前端展示用，如 "账号管理"
    private List<PermissionDTO> permissions;
}
```

- [ ] **Step 7: 创建请求 DTO**

```java
package com.campus.platform.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class CreateRoleRequest {
    @NotBlank private String roleKey;
    @NotBlank private String name;
    private String description;
    @NotNull private String presetKey; // 继承的预设角色 key
    private String status = "ACTIVE";
}

@Data
public class UpdateRoleRequest {
    private String name;
    private String description;
    private String status;
}

@Data
public class UpdateRolePermissionsRequest {
    private List<UUID> permissionIds; // 完整权限列表（覆盖模式）
}
```

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/campus/platform/entity/Role.java
git add backend/src/main/java/com/campus/platform/entity/Permission.java
git add backend/src/main/java/com/campus/platform/entity/RolePermission.java
git add backend/src/main/java/com/campus/platform/entity/dto/RoleDTO.java
git add backend/src/main/java/com/campus/platform/entity/dto/PermissionDTO.java
git add backend/src/main/java/com/campus/platform/entity/dto/PermissionModuleDTO.java
git add backend/src/main/java/com/campus/platform/entity/dto/CreateRoleRequest.java
git add backend/src/main/java/com/campus/platform/entity/dto/UpdateRoleRequest.java
git add backend/src/main/java/com/campus/platform/entity/dto/UpdateRolePermissionsRequest.java
git commit -m "feat(role): add role/permission entities and DTOs"
```

---

## Task 3: 后端 Repository 层

**Files:**
- Create: `backend/src/main/java/com/campus/platform/repository/RoleRepository.java`
- Create: `backend/src/main/java/com/campus/platform/repository/PermissionRepository.java`
- Create: `backend/src/main/java/com/campus/platform/repository/RolePermissionRepository.java`

- [ ] **Step 1: 创建 RoleRepository**

```java
package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleRepository extends BaseMapper<Role> {
}
```

- [ ] **Step 2: 创建 PermissionRepository**

```java
package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionRepository extends BaseMapper<Permission> {
}
```

- [ ] **Step 3: 创建 RolePermissionRepository**

```java
package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.platform.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface RolePermissionRepository extends BaseMapper<RolePermission> {

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") java.util.UUID roleId);
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/campus/platform/repository/RoleRepository.java
git add backend/src/main/java/com/campus/platform/repository/PermissionRepository.java
git add backend/src/main/java/com/campus/platform/repository/RolePermissionRepository.java
git commit -m "feat(role): add repository layer"
```

---

## Task 4: 后端 Security 改造

**Files:**
- Create: `backend/src/main/java/com/campus/platform/security/RequirePermission.java`
- Modify: `backend/src/main/java/com/campus/platform/security/PermissionAspect.java`
- Modify: `backend/src/main/java/com/campus/platform/security/AccountPrincipal.java`
- Modify: `backend/src/main/java/com/campus/platform/security/JwtTokenProvider.java`

- [ ] **Step 1: 创建 @RequirePermission 注解**

```java
package com.campus.platform.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String[] value(); // 所需权限列表，如 {"account:read", "account:create"}
}
```

- [ ] **Step 2: 更新 PermissionAspect，追加权限字符串校验逻辑**

在 `checkRole` 方法之后，增加权限字符串校验段：

```java
// 在 PermissionAspect.java 的 checkRole 方法末尾追加：
@Before("@annotation(requirePermission)")
public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
    AccountPrincipal principal = SecurityContext.get();
    if (principal == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或会话已过期");
    }
    // principal.getPermissions() 已由 JwtTokenProvider 在登录时注入
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
```

> 在 @RequireRole 之前先走 Role 检查，通过后才进 @RequirePermission 检查。
> 如果方法同时标注了两个注解，确保 Aspect 能正确处理（用 @Order 指定）。

- [ ] **Step 3: 更新 AccountPrincipal，增加 permissions 字段**

```java
// AccountPrincipal.java 新增字段
private List<String> permissions;

// Getter/Builder 同步更新
public List<String> getPermissions() {
    return permissions;
}
```

- [ ] **Step 4: 更新 JwtTokenProvider，登录时注入 permissions**

在登录成功/刷新 token 时，从 RoleService 获取该角色的完整权限列表，存入 JWT payload：

```java
// JwtTokenProvider.java - buildToken 方法中追加：
List<String> rolePermissions = roleService.getRolePermissionStrings(principal.getRole());
claims.put("permissions", rolePermissions);
```

在解析 token 时同步读取到 AccountPrincipal。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/campus/platform/security/RequirePermission.java
git add backend/src/main/java/com/campus/platform/security/PermissionAspect.java
git add backend/src/main/java/com/campus/platform/security/AccountPrincipal.java
git add backend/src/main/java/com/campus/platform/security/JwtTokenProvider.java
git commit -m "feat(security): add @RequirePermission and permissions in JWT"
```

---

## Task 5: 后端 Service 层

**Files:**
- Create: `backend/src/main/java/com/campus/platform/service/RoleService.java`

- [ ] **Step 1: 创建 RoleService**

```java
package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.Permission;
import com.campus.platform.entity.Role;
import com.campus.platform.entity.RolePermission;
import com.campus.platform.entity.dto.*;
import com.campus.platform.entity.enums.AccountRole;
import com.campus.platform.exception.BusinessException;
import com.campus.platform.exception.ErrorCode;
import com.campus.platform.repository.PermissionRepository;
import com.campus.platform.repository.RolePermissionRepository;
import com.campus.platform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    private static final Map<String, String> MODULE_LABELS = Map.ofEntries(
            Map.entry("account", "账号管理"),
            Map.entry("major", "专业配置"),
            Map.entry("quota", "名额管理"),
            Map.entry("verification", "成绩核验"),
            Map.entry("checkin", "报到管理"),
            Map.entry("report", "数据报表")
    );

    // 受限权限：不可分配给自定义角色
    private static final Set<String> RESTRICTED_PERMS = Set.of(
            "account:create", "account:edit", "account:disable"
    );

    /** 获取所有角色（带合并后的权限） */
    public List<RoleDTO> listRoles() {
        List<Role> roles = roleRepository.selectList(null);
        return roles.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** 获取角色详情 */
    public RoleDTO getRole(UUID id) {
        Role role = roleRepository.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        return toDTO(role);
    }

    /** 新增自定义角色（继承预设权限） */
    @Transactional
    public RoleDTO createRole(CreateRoleRequest req) {
        // 检查 roleKey 唯一
        long count = roleRepository.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, req.getRoleKey()));
        if (count > 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "角色标识已存在");

        Role role = new Role();
        role.setRoleKey(req.getRoleKey());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setIsPreset(false);
        role.setPresetKey(req.getPresetKey());
        role.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        roleRepository.insert(role);

        // 从预设角色复制权限（is_explicit=false）
        copyPermissionsFromPreset(role, req.getPresetKey());
        return toDTO(role);
    }

    /** 更新角色基本信息 */
    @Transactional
    public RoleDTO updateRole(UUID id, UpdateRoleRequest req) {
        Role role = roleRepository.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        if (Boolean.TRUE.equals(role.getIsPreset())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "预设角色不可修改");
        }
        if (req.getName() != null) role.setName(req.getName());
        if (req.getDescription() != null) role.setDescription(req.getDescription());
        if (req.getStatus() != null) role.setStatus(req.getStatus());
        roleRepository.updateById(role);
        return toDTO(role);
    }

    /** 删除自定义角色 */
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        if (Boolean.TRUE.equals(role.getIsPreset())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "预设角色不可删除");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    /** 更新角色权限（覆盖模式） */
    @Transactional
    public RoleDTO updateRolePermissions(UUID id, UpdateRolePermissionsRequest req) {
        Role role = roleRepository.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        if (Boolean.TRUE.equals(role.getIsPreset())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "预设角色不可修改权限");
        }

        // 删除旧关联
        rolePermissionRepository.deleteByRoleId(id);

        // 获取预设角色权限（is_explicit=false）
        Set<UUID> presetPermIds = getPresetPermissionIds(role.getPresetKey());

        // 插入新关联
        Set<UUID> newPermIds = new HashSet<>(req.getPermissionIds());
        for (UUID permId : newPermIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permId);
            rp.setIsExplicit(!presetPermIds.contains(permId)); // 不在预设中的为显式新增
            rolePermissionRepository.insert(rp);
        }
        return toDTO(roleRepository.selectById(id));
    }

    /** 获取权限树 */
    public List<PermissionModuleDTO> getPermissionModules() {
        List<Permission> all = permissionRepository.selectList(null);
        Map<String, List<PermissionDTO>> grouped = all.stream()
                .map(p -> {
                    PermissionDTO dto = new PermissionDTO();
                    dto.setId(p.getId());
                    dto.setModule(p.getModule());
                    dto.setAction(p.getAction());
                    dto.setLabel(p.getLabel());
                    dto.setIsRestricted(RESTRICTED_PERMS.contains(p.getModule() + ":" + p.getAction()));
                    dto.setIsExplicit(false);
                    return dto;
                })
                .collect(Collectors.groupingBy(PermissionDTO::getModule));

        return grouped.entrySet().stream()
                .map(e -> {
                    PermissionModuleDTO m = new PermissionModuleDTO();
                    m.setModule(e.getKey());
                    m.setModuleLabel(MODULE_LABELS.getOrDefault(e.getKey(), e.getKey()));
                    m.setPermissions(e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 根据 role_key 返回权限字符串列表（供 JWT 使用） */
    public List<String> getRolePermissionStrings(String roleKey) {
        Role role = roleRepository.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, roleKey));
        if (role == null) {
            // 兜底：使用旧枚举逻辑
            return getLegacyPermissions(roleKey);
        }
        List<PermissionDTO> perms = toDTO(role).getPermissions();
        return perms.stream()
                .filter(PermissionDTO::getIsExplicit)
                .map(p -> p.getModule() + ":" + p.getAction())
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setRoleKey(role.getRoleKey());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setIsPreset(role.getIsPreset());
        dto.setPresetKey(role.getPresetKey());
        dto.setStatus(role.getStatus());

        // 获取该角色的所有关联权限
        List<RolePermission> rps = rolePermissionRepository.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
        Set<UUID> permIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
        Map<UUID, Boolean> explicitMap = rps.stream()
                .collect(Collectors.toMap(RolePermission::getPermissionId, RolePermission::getIsExplicit));

        List<Permission> allPerms = permissionRepository.selectList(null);
        List<PermissionDTO> permDTOs = allPerms.stream().map(p -> {
            PermissionDTO pd = new PermissionDTO();
            pd.setId(p.getId());
            pd.setModule(p.getModule());
            pd.setAction(p.getAction());
            pd.setLabel(p.getLabel());
            pd.setIsExplicit(permIds.contains(p.getId()));
            pd.setIsRestricted(RESTRICTED_PERMS.contains(p.getModule() + ":" + p.getAction()));
            return pd;
        }).collect(Collectors.toList());
        dto.setPermissions(permDTOs);
        return dto;
    }

    private void copyPermissionsFromPreset(Role newRole, String presetKey) {
        List<Role> presets = roleRepository.selectList(
                new LambdaQueryWrapper<Role>().eq(Role::getIsPreset, true));
        Role preset = presets.stream()
                .filter(r -> r.getRoleKey().equals(presetKey))
                .findFirst().orElse(null);
        if (preset == null) return;

        List<RolePermission> presetRPs = rolePermissionRepository.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, preset.getId()));
        for (RolePermission rp : presetRPs) {
            RolePermission copy = new RolePermission();
            copy.setRoleId(newRole.getId());
            copy.setPermissionId(rp.getPermissionId());
            copy.setIsExplicit(false);
            rolePermissionRepository.insert(copy);
        }
    }

    private Set<UUID> getPresetPermissionIds(String presetKey) {
        if (presetKey == null) return Set.of();
        Role preset = roleRepository.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getIsPreset, true)
                        .eq(Role::getRoleKey, presetKey));
        if (preset == null) return Set.of();
        List<RolePermission> rps = rolePermissionRepository.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, preset.getId()));
        return rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
    }

    // 旧枚举兜底逻辑（preset_key 不存在时）
    private List<String> getLegacyPermissions(String roleKey) {
        return switch (roleKey) {
            case "OP_ADMIN" -> List.of("*");
            case "SCHOOL_ADMIN" -> List.of(
                    "account:read", "account:create", "account:edit", "account:disable",
                    "major:read", "major:create", "major:edit", "major:disable",
                    "quota:read", "quota:create", "quota:edit",
                    "verification:read", "verification:create",
                    "checkin:read", "checkin:material", "checkin:confirm",
                    "report:read");
            case "SCHOOL_STAFF" -> List.of(
                    "major:read", "quota:read",
                    "verification:read",
                    "checkin:read", "checkin:material",
                    "report:read");
            default -> List.of();
        };
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/campus/platform/service/RoleService.java
git commit -m "feat(role): add RoleService with CRUD and permission inheritance"
```

---

## Task 6: 后端 Controller 层

**Files:**
- Modify: `backend/src/main/java/com/campus/platform/controller/RoleController.java`
- Create: `backend/src/main/java/com/campus/platform/controller/PermissionController.java`

- [ ] **Step 1: 更新 RoleController（替换现有内容）**

```java
package com.campus.platform.controller;

import com.campus.platform.entity.dto.*;
import com.campus.platform.security.RequireRole;
import com.campus.platform.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<RoleDTO>> list() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public ResponseEntity<RoleDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRole(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @RequireRole("OP_ADMIN")
    public ResponseEntity<RoleDTO> create(@Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.ok(roleService.createRole(req));
    }

    @Operation(summary = "更新角色信息")
    @PutMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public ResponseEntity<RoleDTO> update(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateRoleRequest req) {
        return ResponseEntity.ok(roleService.updateRole(id, req));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @RequireRole("OP_ADMIN")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/{id}/permissions")
    @RequireRole("OP_ADMIN")
    public ResponseEntity<RoleDTO> updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest req) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, req));
    }
}
```

- [ ] **Step 2: 创建 PermissionController**

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/campus/platform/controller/RoleController.java
git add backend/src/main/java/com/campus/platform/controller/PermissionController.java
git commit -m "feat(role): update RoleController and add PermissionController"
```

---

## Task 7: 前端 API 层

**Files:**
- Modify: `frontend/src/api/role.ts`
- Create: `frontend/src/api/permission.ts`

- [ ] **Step 1: 更新 role.ts**

```typescript
import axios from './axios'
import type { Result } from './axios'

export interface PermissionItem {
  id: string
  module: string
  action: string
  label: string
  isExplicit: boolean
  isRestricted: boolean
}

export interface Role {
  id: string
  roleKey: string
  name: string
  description: string
  isPreset: boolean
  presetKey: string | null
  status: 'ACTIVE' | 'INACTIVE'
  permissions: PermissionItem[]
}

export interface CreateRoleRequest {
  roleKey: string
  name: string
  description?: string
  presetKey: string
  status?: string
}

export interface UpdateRoleRequest {
  name?: string
  description?: string
  status?: string
}

export const getRoles = () =>
  axios.get<Result<Role[]>>('/v1/roles')

export const getRole = (id: string) =>
  axios.get<Result<Role>>(`/v1/roles/${id}`)

export const createRole = (data: CreateRoleRequest) =>
  axios.post<Result<Role>>('/v1/roles', data)

export const updateRole = (id: string, data: UpdateRoleRequest) =>
  axios.put<Result<Role>>(`/v1/roles/${id}`, data)

export const deleteRole = (id: string) =>
  axios.delete(`/v1/roles/${id}`)

export const updateRolePermissions = (id: string, permissionIds: string[]) =>
  axios.put<Result<Role>>(`/v1/roles/${id}/permissions`, { permissionIds })
```

- [ ] **Step 2: 创建 permission.ts**

```typescript
import axios from './axios'
import type { Result } from './axios'

export interface PermissionItem {
  id: string
  module: string
  action: string
  label: string
  isExplicit: boolean
  isRestricted: boolean
}

export interface PermissionModule {
  module: string
  moduleLabel: string
  permissions: PermissionItem[]
}

export const getPermissionTree = () =>
  axios.get<Result<PermissionModule[]>>('/v1/permissions')
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/api/role.ts frontend/src/api/permission.ts
git commit -m "feat(role): update role API and add permission API"
```

---

## Task 8: 前端 Pinia Store

**Files:**
- Create: `frontend/src/stores/permission.ts`

- [ ] **Step 1: 创建 permission store**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPermissionTree } from '@/api/permission'
import type { PermissionModule } from '@/api/permission'

export const usePermissionStore = defineStore('permission', () => {
  const modules = ref<PermissionModule[]>([])
  const loaded = ref(false)

  async function fetchModules() {
    if (loaded.value) return
    const { data } = await getPermissionTree()
    modules.value = data
    loaded.value = true
  }

  return { modules, loaded, fetchModules }
})
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/stores/permission.ts
git commit -m "feat(role): add permission Pinia store"
```

---

## Task 9: 前端 usePermission Hook

**Files:**
- Modify: `frontend/src/composables/usePermission.ts`

- [ ] **Step 1: 更新 usePermission.ts**

```typescript
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const auth = useAuthStore()
  const permStore = usePermissionStore()

  // 初始化权限数据（懒加载）
  async function ensurePermissions() {
    if (auth.role === 'OP_ADMIN') {
      // OP_ADMIN 拥有所有权限，isOpAdmin 时不需要从后端拉取
      return
    }
    await permStore.fetchModules()
  }

  function can(permission: string): boolean {
    if (!auth.role) return false
    if (auth.role === 'OP_ADMIN') return true

    // 从 permission store 获取权限列表
    const modules = permStore.modules
    if (!modules.length) {
      // 未加载时使用旧硬编码兜底
      return false
    }

    for (const mod of modules) {
      for (const p of mod.permissions) {
        if (`${p.module}:${p.action}` === permission && p.isExplicit) {
          return true
        }
      }
    }
    return false
  }

  function canAny(...permissions: string[]): boolean {
    return permissions.some(p => can(p))
  }

  const isOpAdmin = computed(() => auth.role === 'OP_ADMIN')
  const isSchoolAdmin = computed(() => auth.role === 'SCHOOL_ADMIN')
  const isSchoolStaff = computed(() => auth.role === 'SCHOOL_STAFF')

  return { can, canAny, isOpAdmin, isSchoolAdmin, isSchoolStaff, ensurePermissions }
}
```

> 注意：`useAuthStore` 中 loginAction / fetchUserInfo 后应调用 `permStore.fetchModules()`，确保权限数据在用户登录后立即加载。可在 auth store 的 `fetchUserInfo` 末尾追加一行 `if (role !== 'OP_ADMIN') permissionStore.fetchModules()`。

- [ ] **Step 2: 提交**

```bash
git add frontend/src/composables/usePermission.ts frontend/src/stores/auth.ts
git commit -m "feat(role): update usePermission to use runtime data"
```

---

## Task 10: 前端 RoleManage.vue

**Files:**
- Modify: `frontend/src/views/admin/RoleManage.vue`

这是最大的一个任务。组件采用左右布局：
- **左侧**：角色列表（预设+自定义）
- **右侧**：角色详情/编辑区

- [ ] **Step 1: 替换 RoleManage.vue 全部内容**

```vue
<template>
  <div class="role-manage">
    <!-- 左侧：角色列表 -->
    <div class="role-list-panel">
      <div class="panel-header">
        <span class="panel-title">角色列表</span>
        <el-button type="primary" size="small" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>

      <!-- 预设角色 -->
      <div class="role-section-label">预设角色</div>
      <div
        v-for="role in presetRoles"
        :key="role.id"
        class="role-item preset"
        :class="{ active: selectedRole?.id === role.id }"
        @click="selectRole(role)"
      >
        <el-icon class="lock-icon"><Lock /></el-icon>
        <div class="role-info">
          <span class="role-name">{{ role.name }}</span>
          <el-tag size="small" :type="getPresetTagType(role.roleKey)">{{ role.roleKey }}</el-tag>
        </div>
        <span class="perm-count">{{ role.permissions.filter(p => p.isExplicit).length }} 项权限</span>
      </div>

      <!-- 自定义角色 -->
      <div class="role-section-label">自定义角色</div>
      <el-empty v-if="customRoles.length === 0" description="暂无自定义角色" :image-size="60" />
      <div
        v-for="role in customRoles"
        :key="role.id"
        class="role-item custom"
        :class="{ active: selectedRole?.id === role.id }"
        @click="selectRole(role)"
      >
        <div class="role-info">
          <span class="role-name">{{ role.name }}</span>
        </div>
        <div class="role-actions" @click.stop>
          <el-button type="primary" link size="small" @click="openEditDialog(role)">编辑</el-button>
          <el-popconfirm title="确定删除该角色？" @confirm="handleDelete(role.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- 右侧：角色详情 + 权限矩阵 -->
    <div class="role-detail-panel">
      <template v-if="selectedRole">
        <div class="detail-header">
          <div>
            <h3 class="detail-title">{{ selectedRole.name }}</h3>
            <span class="detail-key">{{ selectedRole.roleKey }}</span>
          </div>
          <el-tag v-if="selectedRole.isPreset" type="info">预设角色</el-tag>
          <el-tag v-else type="success">自定义角色</el-tag>
        </div>

        <div class="detail-desc">{{ selectedRole.description || '暂无描述' }}</div>

        <!-- 权限矩阵 -->
        <div class="permission-matrix">
          <div class="matrix-header">
            <span>权限分配</span>
            <template v-if="!selectedRole.isPreset">
              <el-button v-if="!isEditing" type="primary" size="small" @click="startEdit">
                编辑权限
              </el-button>
              <template v-else>
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button type="primary" size="small" @click="savePermissions" :loading="saving">
                  保存
                </el-button>
              </template>
            </template>
          </div>

          <el-collapse v-model="activeModules">
            <el-collapse-item
              v-for="mod in permissionModules"
              :key="mod.module"
              :title="mod.moduleLabel"
              :name="mod.module"
            >
              <div class="perm-list">
                <div
                  v-for="perm in mod.permissions"
                  :key="perm.id"
                  class="perm-row"
                >
                  <el-checkbox
                    v-model="perm.isExplicit"
                    :disabled="isCheckboxDisabled(perm)"
                  >
                    {{ perm.label }}
                  </el-checkbox>
                  <el-tag v-if="perm.isRestricted" size="small" type="warning">系统保留</el-tag>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </template>

      <el-empty v-else description="请从左侧选择一个角色" />
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="500px"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="角色标识" prop="roleKey" v-if="dialogMode === 'create'">
          <el-input v-model="form.roleKey" placeholder="如 CUSTOM_001" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="角色展示名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="继承模板" prop="presetKey" v-if="dialogMode === 'create'">
          <el-select v-model="form.presetKey" placeholder="选择继承的预设角色">
            <el-option label="运营管理员（全部权限）" value="OP_ADMIN" />
            <el-option label="院校管理员" value="SCHOOL_ADMIN" />
            <el-option label="院校工作人员（只读）" value="SCHOOL_STAFF" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="dialogMode === 'edit'">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { Plus, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { usePermissionStore } from '@/stores/permission'
import {
  getRoles, createRole, updateRole, deleteRole,
  updateRolePermissions, getPermissionTree
} from '@/api/role'
import type { Role, PermissionModule } from '@/api/permission'
import { getPermissionTree as getPermTree } from '@/api/permission'
import type { CreateRoleRequest, UpdateRoleRequest } from '@/api/role'

const permStore = usePermissionStore()

// 角色列表
const roles = ref<Role[]>([])
const selectedRole = ref<Role | null>(null)
const permissionModules = ref<PermissionModule[]>([])

// 编辑状态
const isEditing = ref(false)
const saving = ref(false)
const editingPermissions = ref<PermissionModule[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<CreateRoleRequest & UpdateRoleRequest>({
  roleKey: '',
  name: '',
  description: '',
  presetKey: 'SCHOOL_ADMIN',
  status: 'ACTIVE',
})
const formRules: FormRules = {
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  presetKey: [{ required: true, message: '请选择继承模板', trigger: 'change' }],
}

// 折叠
const activeModules = ref<string[]>([])

const presetRoles = computed(() => roles.value.filter(r => r.isPreset))
const customRoles = computed(() => roles.value.filter(r => !r.isPreset))

onMounted(async () => {
  await loadData()
})

async function loadData() {
  const [{ data: roleList }, { data: permModules }] = await Promise.all([
    getRoles(),
    getPermTree(),
  ])
  roles.value = roleList
  permissionModules.value = permModules

  if (roles.value.length > 0 && !selectedRole.value) {
    selectRole(roles.value[0])
  }
}

function selectRole(role: Role) {
  if (isEditing.value) {
    ElMessageBox.confirm('当前有未保存的修改，是否放弃？', '提示', { type: 'warning' })
      .then(() => { selectedRole.value = role; resetEdit() })
      .catch(() => {})
    return
  }
  selectedRole.value = role
}

function getPresetTagType(key: string): '' | 'primary' | 'success' | 'warning' {
  return key === 'OP_ADMIN' ? 'primary' : key === 'SCHOOL_ADMIN' ? 'success' : 'warning'
}

function isCheckboxDisabled(perm: { isRestricted: boolean; module: string; action: string }) {
  if (!selectedRole.value?.isPreset) return false
  // 预设角色只读
  return true
}

function openCreateDialog() {
  dialogMode.value = 'create'
  Object.assign(form, { roleKey: '', name: '', description: '', presetKey: 'SCHOOL_ADMIN', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEditDialog(role: Role) {
  dialogMode.value = 'edit'
  Object.assign(form, { name: role.name, description: role.description || '', status: role.status })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      const { data } = await createRole(form)
      roles.value.push(data)
      ElMessage.success('角色创建成功')
    } else if (selectedRole.value) {
      const { data } = await updateRole(selectedRole.value.id, form)
      const idx = roles.value.findIndex(r => r.id === data.id)
      if (idx >= 0) roles.value[idx] = data
      if (selectedRole.value.id === data.id) selectedRole.value = data
      ElMessage.success('角色更新成功')
    }
    dialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: string) {
  await deleteRole(id)
  roles.value = roles.value.filter(r => r.id !== id)
  if (selectedRole.value?.id === id) selectedRole.value = roles.value[0] || null
  ElMessage.success('删除成功')
}

function startEdit() {
  if (!selectedRole.value) return
  editingPermissions.value = JSON.parse(JSON.stringify(permissionModules.value))
  // 用选中角色的权限回填 isExplicit
  const permMap = new Map(
    selectedRole.value.permissions.map(p => [`${p.module}:${p.action}`, p.isExplicit])
  )
  for (const mod of editingPermissions.value) {
    for (const p of mod.permissions) {
      p.isExplicit = permMap.get(`${p.module}:${p.action}`) ?? false
    }
  }
  isEditing.value = true
}

function cancelEdit() {
  resetEdit()
}

function resetEdit() {
  isEditing.value = false
  editingPermissions.value = []
}

async function savePermissions() {
  if (!selectedRole.value) return
  saving.value = true
  try {
    // 收集选中的权限ID
    const permissionIds: string[] = []
    for (const mod of editingPermissions.value) {
      for (const p of mod.permissions) {
        if (p.isExplicit) permissionIds.push(p.id)
      }
    }
    const { data } = await updateRolePermissions(selectedRole.value.id, permissionIds)
    const idx = roles.value.findIndex(r => r.id === data.id)
    if (idx >= 0) roles.value[idx] = data
    selectedRole.value = data
    // 同步更新 permissionModules 中的选中状态
    const permSet = new Set(data.permissions.filter((p: any) => p.isExplicit).map((p: any) => `${p.module}:${p.action}`))
    for (const mod of permissionModules.value) {
      for (const p of mod.permissions) {
        p.isExplicit = permSet.has(`${p.module}:${p.action}`)
      }
    }
    ElMessage.success('权限保存成功')
    resetEdit()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.role-manage {
  display: flex;
  height: calc(100vh - 120px);
  gap: 16px;
  padding: 20px;
  overflow: hidden;
}

.role-list-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
}

.role-section-label {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 4px;
}

.role-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.role-item:hover {
  background: #f5f7fa;
}

.role-item.active {
  background: #ecf5ff;
}

.role-item.preset {
  opacity: 0.85;
}

.lock-icon {
  color: #909399;
  margin-right: 8px;
  font-size: 14px;
}

.role-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.role-name {
  font-size: 14px;
  font-weight: 500;
}

.perm-count {
  font-size: 12px;
  color: #909399;
}

.role-actions {
  display: flex;
  gap: 4px;
}

.role-detail-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.detail-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.detail-key {
  font-size: 12px;
  color: #909399;
}

.detail-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}

.permission-matrix {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.matrix-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.perm-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/admin/RoleManage.vue
git commit -m "feat(role): complete rewrite of RoleManage.vue with runtime data"
```

---

## Task 11: 验证

- [ ] **Step 1: 后端验证**

启动后端服务，访问 Swagger UI (`/swagger-ui.html`) 验证：
- GET `/api/v1/roles` 返回预设角色数据
- GET `/api/v1/permissions` 返回权限树
- POST `/api/v1/roles` 创建自定义角色
- PUT `/api/v1/roles/{id}/permissions` 更新权限
- DELETE `/api/v1/roles/{id}` 删除自定义角色
- 尝试修改/删除预设角色，返回 403

- [ ] **Step 2: 前端验证**

启动前端，登录 OP_ADMIN 账号：
- 访问 `/roles` 页面，左侧显示 3 个预设角色
- 点击新增角色，输入信息并选择继承模板，保存后出现在左侧自定义列表
- 点击自定义角色，右侧显示权限矩阵，编辑并保存
- 尝试点击预设角色的编辑权限，确认 checkbox 灰显不可点击
- 尝试删除预设角色，确认无删除按钮

---

## 实施顺序

1. Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8 → Task 9 → Task 10 → Task 11

**注意**：Task 1（数据库迁移）需要先执行，迁移成功后再进行后续任务。
