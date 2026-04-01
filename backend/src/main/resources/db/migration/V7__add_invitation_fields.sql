-- =========================================================
-- V7__add_invitation_fields.sql
-- 新增补录邀请相关字段和全局配置
-- =========================================================

-- 1. 更新 candidate_push 状态约束，新增 INVITED 状态（包含 ENROLLED_ELSEWHERE 以兼容 V6）
ALTER TABLE candidate_push DROP CONSTRAINT IF EXISTS chk_push_status;
ALTER TABLE candidate_push ADD CONSTRAINT chk_push_status CHECK (status IN (
    'PENDING', 'CONDITIONAL', 'ADMITTED', 'CONFIRMED',
    'MATERIAL_RECEIVED', 'CHECKED_IN', 'REJECTED', 'INVALIDATED',
    'ENROLLED_ELSEWHERE', 'INVITED'
));

-- 2. 新增邀请相关字段
ALTER TABLE candidate_push ADD COLUMN IF NOT EXISTS invitation_sent_at TIMESTAMPTZ;
ALTER TABLE candidate_push ADD COLUMN IF NOT EXISTS invitation_expires_at TIMESTAMPTZ;
ALTER TABLE candidate_push ADD COLUMN IF NOT EXISTS invitation_major_id UUID REFERENCES major(major_id) ON DELETE SET NULL;
ALTER TABLE candidate_push ADD COLUMN IF NOT EXISTS invitation_message TEXT;

-- 3. 新增索引
CREATE INDEX IF NOT EXISTS idx_push_invitation_expires ON candidate_push (invitation_expires_at)
    WHERE invitation_expires_at IS NOT NULL;

-- 4. 新增全局配置项
INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'supplement_mode', 'MODE_1')
ON CONFLICT (school_id, config_key) DO NOTHING;

INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'invitation_default_days', '7')
ON CONFLICT (school_id, config_key) DO NOTHING;
