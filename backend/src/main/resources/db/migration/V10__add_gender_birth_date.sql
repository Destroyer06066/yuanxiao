-- =========================================================
-- V9__add_gender_birth_date.sql
-- 新增考生性别和出生日期字段
-- =========================================================

ALTER TABLE candidate_push
ADD COLUMN IF NOT EXISTS gender VARCHAR(10),
ADD COLUMN IF NOT EXISTS birth_date DATE;

COMMENT ON COLUMN candidate_push.gender IS '性别：M（男）/ F（女）/ O（其他）/ NULL（未知，上游系统以后提供）';
COMMENT ON COLUMN candidate_push.birth_date IS '出生日期，NULL表示上游系统以后提供';