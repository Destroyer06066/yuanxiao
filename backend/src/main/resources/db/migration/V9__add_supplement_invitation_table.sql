-- =========================================================
-- V9__add_supplement_invitation_table.sql
-- 补录邀请表，记录模式二的补录邀请
-- =========================================================

-- 补录邀请表
CREATE TABLE IF NOT EXISTS supplement_invitation (
    invitation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id VARCHAR(64) NOT NULL,
    push_id UUID NOT NULL REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    school_id UUID NOT NULL REFERENCES school(school_id) ON DELETE CASCADE,
    invitation_major_id UUID REFERENCES major(major_id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    supplement_round INTEGER NOT NULL DEFAULT 1,
    message TEXT,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_invitation_candidate ON supplement_invitation (candidate_id);
CREATE INDEX IF NOT EXISTS idx_invitation_school ON supplement_invitation (school_id);
CREATE INDEX IF NOT EXISTS idx_invitation_status ON supplement_invitation (status);
CREATE INDEX IF NOT EXISTS idx_invitation_expires ON supplement_invitation (expires_at)
    WHERE expires_at IS NOT NULL;

-- 注释
COMMENT ON TABLE supplement_invitation IS '补录邀请表-模式二';
COMMENT ON COLUMN supplement_invitation.invitation_id IS '邀请记录ID';
COMMENT ON COLUMN supplement_invitation.candidate_id IS '考生ID';
COMMENT ON COLUMN supplement_invitation.push_id IS '关联的推送记录ID';
COMMENT ON COLUMN supplement_invitation.school_id IS '邀请院校ID';
COMMENT ON COLUMN supplement_invitation.invitation_major_id IS '邀请专业ID';
COMMENT ON COLUMN supplement_invitation.status IS '邀请状态：INVITED-已发出邀请 ACCEPTED-已接受 REJECTED-已拒绝 EXPIRED-已过期';
COMMENT ON COLUMN supplement_invitation.supplement_round IS '补录轮次';
COMMENT ON COLUMN supplement_invitation.message IS '邀请留言';
COMMENT ON COLUMN supplement_invitation.sent_at IS '发送时间';
COMMENT ON COLUMN supplement_invitation.expires_at IS '过期时间';
COMMENT ON COLUMN supplement_invitation.responded_at IS '响应时间';
