-- V5__add_supplement_permission.sql
-- 为补录管理模块补充权限数据

-- 补录管理权限
INSERT INTO permission (module, action, label, description)
VALUES ('supplement', 'read', '查看补录', NULL)
ON CONFLICT (module, action) DO NOTHING;

-- 给 OP_ADMIN 和 SCHOOL_ADMIN 赋予补录查看权限
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.role_key IN ('OP_ADMIN', 'SCHOOL_ADMIN')
  AND p.module = 'supplement'
  AND p.action = 'read'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SCHOOL_STAFF 也赋予补录查看权限（只读）
INSERT INTO role_permission (role_id, permission_id, is_explicit)
SELECT r.id, p.id, false
FROM role r, permission p
WHERE r.role_key = 'SCHOOL_STAFF'
  AND p.module = 'supplement'
  AND p.action = 'read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
