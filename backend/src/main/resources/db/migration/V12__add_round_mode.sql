-- =========================================================
-- V11__add_round_mode.sql
-- 新增补录轮次模式字段，按轮次区分模式一/模式二
-- 第1轮=模式一（考生推送），第2轮及以后=模式二（邀请模式）
-- =========================================================

ALTER TABLE supplement_round
ADD COLUMN IF NOT EXISTS mode VARCHAR(20) DEFAULT 'MODE_1';

COMMENT ON COLUMN supplement_round.mode IS '轮次模式：MODE_1（考生推送模式）/ MODE_2（邀请模式），由轮次号自动决定，不允许手动修改';

-- 将现有的所有轮次 mode 设置为 MODE_1（保持向后兼容）
UPDATE supplement_round SET mode = 'MODE_1' WHERE mode IS NULL;

-- 为已有数据设置默认值
ALTER TABLE supplement_round ALTER COLUMN mode SET DEFAULT 'MODE_1';
ALTER TABLE supplement_round ALTER COLUMN mode SET NOT NULL;
