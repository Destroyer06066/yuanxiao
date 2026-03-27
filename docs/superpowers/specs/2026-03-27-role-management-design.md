# 角色权限管理模块设计文档

## 1. 目标

为院校管理平台新增完整的角色权限管理模块，支持角色的增删改查和按钮级权限分配。运营管理员（OP_ADMIN）可管理所有角色，普通用户无权限访问此模块。

## 2. 现状

- 角色系统基于枚举 `AccountRole`（OP_ADMIN, SCHOOL_ADMIN, SCHOOL_STAFF），硬编码不可扩展
- 前端 `RoleManage.vue` 为静态展示页面，无任何后端交互
- 权限以字符串形式定义在 `usePermission.ts` 中，但后端仅做角色级校验，未精确到权限字符串

## 3. 数据模型

### 3.1 角色表 (role)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | UUID | PK | 主键 |
| role_key | VARCHAR(50) | UNIQUE, NOT NULL | 角色唯一标识，如 `CUSTOM_001` |
| name | VARCHAR(50) | NOT NULL | 展示名称 |
| description | VARCHAR(200) | | 描述 |
| is_preset | BOOLEAN | NOT NULL DEFAULT false | true=预设角色不可删/改 |
| preset_key | VARCHAR(50) | | 继承的预设角色 key（仅自定义角色有） |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / INACTIVE |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

### 3.2 权限表 (permission)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|------|
| id | UUID | PK | 主键 |
| module | VARCHAR(50) | NOT NULL | 模块名，如 `account` |
| action | VARCHAR(50) | NOT NULL | 操作名，如 `read/create/edit/disable` |
| label | VARCHAR(100) | NOT NULL | 前端展示标签，如 `查看账号` |
| description | VARCHAR(200) | | 描述 |
| UNIQUE | | (module, action) | 唯一约束 |

### 3.3 角色-权限关联表 (role_permission)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| role_id | UUID | PK, FK -> role.id | 角色ID |
| permission_id | UUID | PK, FK -> permission.id | 权限ID |
| is_explicit | BOOLEAN | NOT NULL DEFAULT true | true=显式分配；false=继承自预设 |
| UNIQUE | | (role_id, permission_id) | |

### 3.4 预设权限数据

基于现有 `usePermission.ts` 中的 17 个权限粒度：

| module | action | label |
|--------|--------|-------|
| account | read | 查看账号 |
| account | create | 新增账号 |
| account | edit | 编辑账号 |
| account | disable | 禁用/启用账号 |
| major | read | 查看专业 |
| major | create | 新增专业 |
| major | edit | 编辑专业 |
| major | disable | 停用/启用专业 |
| quota | read | 查看名额 |
| quota | create | 新增名额 |
| quota | edit | 编辑名额 |
| verification | read | 查看核验 |
| verification | create | 提交核验 |
| checkin | read | 查看报到 |
| checkin | material | 材料收件登记 |
| checkin | confirm | 确认报到 |
| report | read | 查看报表 |

### 3.5 预设角色

| role_key | name | preset_key | permissions |
|----------|------|-----------|------------|
| OP_ADMIN | 运营管理员 | (null) | 全部 17 个权限（is_explicit=false） |
| SCHOOL_ADMIN | 院校管理员 | (null) | 全部权限含 account:read |
| SCHOOL_STAFF | 院校工作人员 | (null) | 读权限（major:read, quota:read, score-line:read, verification:read, checkin:read, report:read） |

> 注：SCHOOL_ADMIN 角色的 `account:read` 在前端列表中灰显（不可分配给自定义角色），account:create/edit/disable 不展示给前端。

## 4. 权限继承模型

- **预设角色**：直接存储全部权限，`is_preset=true`，不可修改/删除
- **自定义角色**：存储 `preset_key`（继承某个预设角色），权限 = 预设权限 + 显式增/删的权限
  - 新增时：自动带入所选预设角色的全部权限
  - 编辑时：可在此基础上增删权限
  - `is_explicit=true` 表示显式分配/移除，`is_explicit=false` 表示从预设继承

## 5. API 设计

### 5.1 获取角色列表

```
GET /api/v1/roles
Auth: OP_ADMIN
Response: RoleDTO[]  // 含 computed 后的完整权限列表
```

### 5.2 获取角色详情

```
GET /api/v1/roles/{id}
Auth: OP_ADMIN
Response: RoleDTO  // 含 permissions[], isExplicit[]
```

### 5.3 新增角色

```
POST /api/v1/roles
Auth: OP_ADMIN
Body: { roleKey, name, description, presetKey, status }
Response: RoleDTO
```

### 5.4 更新角色基本信息

```
PUT /api/v1/roles/{id}
Auth: OP_ADMIN
Body: { name?, description?, status? }
Constraint: 预设角色返回 403
```

### 5.5 删除角色

```
DELETE /api/v1/roles/{id}
Auth: OP_ADMIN
Constraint: 预设角色返回 403
```

### 5.6 更新角色权限

```
PUT /api/v1/roles/{id}/permissions
Auth: OP_ADMIN
Body: { permissionIds: UUID[] }  // 完整权限列表（覆盖模式）
Response: RoleDTO
```

### 5.7 获取权限树

```
GET /api/v1/permissions
Auth: OP_ADMIN
Response: PermissionModuleDTO[]
  [{
    module: "account",
    moduleLabel: "账号管理",
    permissions: [{ id, action, label, isRestricted }]
  }]
```

`isRestricted=true` 表示该权限不可分配给自定义角色（如 account:create/edit/disable 仅预设角色可用）。

## 6. 后端改造

### 6.1 @RequirePermission 注解

新增 `@RequirePermission` 注解，在 `PermissionAspect` 中拦截并校验权限字符串。

### 6.2 AccountPrincipal 扩展

在 JWT payload 和 AccountPrincipal 中增加 `permissions: string[]` 字段，登录时由后端计算并注入。

### 6.3 SecurityConfig

放开 `/api/v1/permissions`（GET）给 OP_ADMIN；角色管理接口由 `@RequireRole("OP_ADMIN")` 保护。

## 7. 前端改造

### 7.1 API 层

- `api/role.ts`：角色 CRUD + 权限更新
- `api/permission.ts`：权限树获取

### 7.2 Pinia Store

新增 `stores/permission.ts`：存储权限列表，供 `usePermission` hook 使用。

### 7.3 usePermission Hook

从 Pinia store 读取权限列表（而非硬编码的 `ROLE_PERMISSIONS`）。首次访问时从后端加载并缓存。

### 7.4 RoleManage.vue

重构为左右布局：
- **左侧角色列表**：预设角色（锁图标灰显）+ 自定义角色
- **右侧角色详情**：
  - 新建模式：输入基本信息 + 选择继承的预设角色
  - 编辑模式：基本信息（预设不可改）+ 权限矩阵（按模块分组）
- **权限矩阵**：每个模块一个折叠面板，每个权限一行 checkbox
  - 预设角色：只读
  - 自定义角色：可编辑，checkbox 勾选/取消
- **顶部**：新增自定义角色按钮

### 7.5 路由

`/roles` 路由保持不变（OP_ADMIN 专属），仅替换 RoleManage.vue 组件内容。

## 8. 错误处理

- 预设角色操作（修改/删除）→ HTTP 403，提示"预设角色不可修改/删除"
- 角色名/key 重复 → HTTP 400，提示具体冲突字段
- 权限不足 → HTTP 403

## 9. 测试要点

- 新增自定义角色，验证权限自动继承
- 编辑自定义角色权限，验证增减正确
- 删除自定义角色，验证关联数据一并清理
- 尝试修改预设角色，返回 403
- 前端权限矩阵与后端数据一致
