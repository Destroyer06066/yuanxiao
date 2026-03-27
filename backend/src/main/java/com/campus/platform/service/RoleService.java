package com.campus.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.platform.entity.Permission;
import com.campus.platform.entity.Role;
import com.campus.platform.entity.RolePermission;
import com.campus.platform.entity.dto.*;
import com.campus.platform.common.exception.BusinessException;
import com.campus.platform.common.exception.ErrorCode;
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
        if (role == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        return toDTO(role);
    }

    /** 新增自定义角色（继承预设权限） */
    @Transactional
    public RoleDTO createRole(CreateRoleRequest req) {
        long count = roleRepository.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, req.getRoleKey()));
        if (count > 0) throw new BusinessException(ErrorCode.INVALID_PARAMETER, "角色标识已存在");

        Role role = new Role();
        role.setRoleKey(req.getRoleKey());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setIsPreset(false);
        role.setPresetKey(req.getPresetKey());
        role.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        roleRepository.insert(role);

        copyPermissionsFromPreset(role, req.getPresetKey());
        return toDTO(role);
    }

    /** 更新角色基本信息 */
    @Transactional
    public RoleDTO updateRole(UUID id, UpdateRoleRequest req) {
        Role role = roleRepository.selectById(id);
        if (role == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
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
        if (role == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
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
        if (role == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        if (Boolean.TRUE.equals(role.getIsPreset())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "预设角色不可修改权限");
        }

        rolePermissionRepository.deleteByRoleId(id);

        Set<UUID> presetPermIds = getPresetPermissionIds(role.getPresetKey());

        Set<UUID> newPermIds = new HashSet<>(req.getPermissionIds());
        for (UUID permId : newPermIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permId);
            rp.setIsExplicit(!presetPermIds.contains(permId));
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

        List<RolePermission> rps = rolePermissionRepository.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
        Set<UUID> permIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());

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
        if (presetKey == null) return;
        Role preset = roleRepository.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getIsPreset, true)
                        .eq(Role::getRoleKey, presetKey));
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
