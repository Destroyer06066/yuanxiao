-- 新增录取截止时间字段
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS deadline TIMESTAMPTZ;

COMMENT ON COLUMN admission_quota.deadline IS '录取截止时间';
