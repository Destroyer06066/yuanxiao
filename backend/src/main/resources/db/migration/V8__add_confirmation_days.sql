-- =========================================================
-- V8__add_confirmation_days.sql
-- 新增录取确认有效期参数
-- =========================================================

-- 首轮录取确认有效期（天）
INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'first_round_confirmation_days', '7')
ON CONFLICT (school_id, config_key) DO NOTHING;

-- 补录确认有效期（天），2轮及以后使用
INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'supplement_round_confirmation_days', '7')
ON CONFLICT (school_id, config_key) DO NOTHING;
