-- =========================================================
-- V9__add_candidate_score_params.sql
-- 新增考生相关参数
-- =========================================================

-- 考生可推送院校数量上限
INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'max_schools_per_candidate', '5')
ON CONFLICT (school_id, config_key) DO NOTHING;

-- 成绩有效期（天）
INSERT INTO school_config (config_id, school_id, config_key, config_value)
VALUES (gen_random_uuid(), NULL, 'score_validity_days', '365')
ON CONFLICT (school_id, config_key) DO NOTHING;
