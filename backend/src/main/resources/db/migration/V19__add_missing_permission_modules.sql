-- V19__add_missing_permission_modules.sql
-- 为权限树新增缺失的模块权限（与 Layout.vue 侧边栏对齐）

-- 站内通知权限
INSERT INTO permission (module, action, label, description)
VALUES ('notification', 'read', '查看通知', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 操作日志权限
INSERT INTO permission (module, action, label, description)
VALUES ('audit', 'read', '查看日志', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 系统参数权限
INSERT INTO permission (module, action, label, description)
VALUES ('system', 'read', '查看参数', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 招生简章权限
INSERT INTO permission (module, action, label, description)
VALUES ('brochure', 'read', '查看简章', NULL)
ON CONFLICT (module, action) DO NOTHING;
INSERT INTO permission (module, action, label, description)
VALUES ('brochure', 'create', '新增简章', NULL)
ON CONFLICT (module, action) DO NOTHING;
INSERT INTO permission (module, action, label, description)
VALUES ('brochure', 'edit', '编辑简章', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 首页权限
INSERT INTO permission (module, action, label, description)
VALUES ('dashboard', 'read', '查看首页', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 给所有预设角色赋予新增权限
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.is_preset = true
  AND p.module IN ('notification', 'audit', 'system', 'brochure', 'dashboard')
ON CONFLICT (role_id, permission_id) DO NOTHING;
