ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ;
COMMENT ON COLUMN admission_quota.start_time IS '录取开始时间';
