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

INSERT INTO role (role_key, name, description, is_preset, preset_key, status)
VALUES ('OP_ADMIN', '运营管理员', '拥有系统全部权限，可管理所有院校、账号和数据。', true, NULL, 'ACTIVE');

INSERT INTO role (role_key, name, description, is_preset, preset_key, status)
VALUES ('SCHOOL_ADMIN', '院校管理员', '管理本校的账号、专业、名额、成绩核验和报到确认等业务操作。', true, NULL, 'ACTIVE');

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

-- ===== SCHOOL_ADMIN: 全部权限 =====

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
