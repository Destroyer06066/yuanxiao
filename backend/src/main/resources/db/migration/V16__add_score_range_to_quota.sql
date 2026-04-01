-- 添加录取分线区间字段到名额表
ALTER TABLE admission_quota
ADD COLUMN IF NOT EXISTS min_score INTEGER,
ADD COLUMN IF NOT EXISTS max_score INTEGER;

COMMENT ON COLUMN admission_quota.min_score IS '录取最低分';
COMMENT ON COLUMN admission_quota.max_score IS '录取最高分';
