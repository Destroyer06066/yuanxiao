-- ============================================================
-- 演示数据初始化脚本
-- 运行方式: psql -U xiaoxin -d campus_platform -f demo-data.sql
-- ============================================================

-- 清理旧演示数据（可选，先不执行防止误删）
-- DELETE FROM candidate_push WHERE deleted = 0;
-- DELETE FROM major WHERE deleted = 0;
-- DELETE FROM admission_quota WHERE deleted = 0;
-- DELETE FROM score_line WHERE deleted = 0;
-- DELETE FROM notification WHERE deleted = 0;
-- DELETE FROM operation_log;

-- ============================================================
-- Step 1: 添加专业
-- ============================================================

-- 清华大学专业
INSERT INTO major (major_id, school_id, major_name, degree_level, status, created_at, updated_at, deleted) VALUES
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '计算机科学与技术', '本科', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '计算机科学与技术', '硕士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '国际关系', '硕士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '阿拉伯语语言文学', '硕士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '新闻传播学', '本科', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', '电子工程', '本科', 'ACTIVE', NOW(), NOW(), 0);

-- 北京大学专业
INSERT INTO major (major_id, school_id, major_name, degree_level, status, created_at, updated_at, deleted) VALUES
(gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', '计算机科学与技术', '本科', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', '计算机科学与技术', '硕士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', '国际关系', '博士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', '法律硕士', '硕士', 'ACTIVE', NOW(), NOW(), 0),
(gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', '经济管理', '本科', 'ACTIVE', NOW(), NOW(), 0);

-- ============================================================
-- Step 2: 添加名额
-- ============================================================

-- 获取清华专业ID
DO $$
DECLARE
    qinghua_cs_u uuid;
    qinghua_cs_g uuid;
    qinghua_ir uuid;
    qinghua_arab uuid;
    qinghua_jc uuid;
    qinghua_ee uuid;
BEGIN
    SELECT major_id INTO qinghua_cs_u FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO qinghua_cs_g FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_ir FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '国际关系' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_arab FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '阿拉伯语语言文学' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_jc FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '新闻传播学' AND degree_level = '本科';
    SELECT major_id INTO qinghua_ee FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '电子工程' AND degree_level = '本科';

    INSERT INTO admission_quota (quota_id, school_id, major_id, year, total_quota, admitted_count, reserved_count, version, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_u, 2026, 50, 12, 3, 0, NOW(), NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_g, 2026, 30, 8, 2, 0, NOW(), NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ir, 2026, 15, 5, 0, 0, NOW(), NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_arab, 2026, 10, 2, 1, 0, NOW(), NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_jc, 2026, 40, 10, 0, 0, NOW(), NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ee, 2026, 35, 15, 2, 0, NOW(), NOW(), 0);

    -- 获取北大专业ID
    DECLARE
        pku_cs_u uuid;
        pku_cs_g uuid;
        pku_ir_d uuid;
        pku_law uuid;
        pku_eco uuid;
    BEGIN
        SELECT major_id INTO pku_cs_u FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '计算机科学与技术' AND degree_level = '本科';
        SELECT major_id INTO pku_cs_g FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '计算机科学与技术' AND degree_level = '硕士';
        SELECT major_id INTO pku_ir_d FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '国际关系' AND degree_level = '博士';
        SELECT major_id INTO pku_law FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '法律硕士' AND degree_level = '硕士';
        SELECT major_id INTO pku_eco FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '经济管理' AND degree_level = '本科';

        INSERT INTO admission_quota (quota_id, school_id, major_id, year, total_quota, admitted_count, reserved_count, version, created_at, updated_at, deleted) VALUES
        (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_cs_u, 2026, 45, 20, 5, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_cs_g, 2026, 25, 10, 3, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_ir_d, 2026, 8, 3, 0, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_law, 2026, 20, 7, 1, 0, NOW(), NOW(), 0),
        (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_eco, 2026, 38, 18, 2, 0, NOW(), NOW(), 0);
    END;
END;
$$;

-- ============================================================
-- Step 3: 添加考生（多状态分布）
-- ============================================================

DO $$
DECLARE
    -- 清华大学
    qinghua_cs_maj_id uuid;
    qinghua_ir_maj_id uuid;
    qinghua_arab_maj_id uuid;
    qinghua_jc_maj_id uuid;
    qinghua_ee_maj_id uuid;
    -- 北京大学
    pku_cs_maj_id uuid;
    pku_law_maj_id uuid;
    pku_eco_maj_id uuid;
    pku_ir_maj_id uuid;
    -- 账号ID
    qinghua_admin_id uuid;
    pku_admin_id uuid;
    qinghua_staff_id uuid;
BEGIN
    SELECT major_id INTO qinghua_cs_maj_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO qinghua_ir_maj_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '国际关系' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_arab_maj_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '阿拉伯语语言文学' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_jc_maj_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '新闻传播学' AND degree_level = '本科';
    SELECT major_id INTO qinghua_ee_maj_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '电子工程' AND degree_level = '本科';
    SELECT major_id INTO pku_cs_maj_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO pku_law_maj_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '法律硕士' AND degree_level = '硕士';
    SELECT major_id INTO pku_eco_maj_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '经济管理' AND degree_level = '本科';
    SELECT major_id INTO pku_ir_maj_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '国际关系' AND degree_level = '博士';
    SELECT account_id INTO qinghua_admin_id FROM account WHERE username = 'school_admin2';
    SELECT account_id INTO pku_admin_id FROM account WHERE username = 'op_admin';
    SELECT account_id INTO qinghua_staff_id FROM account WHERE username = 'test_staff_001';

    -- ===== 清华大学考生 =====

    -- PENDING: 3人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, push_round, pushed_at, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-001', '李明', '中国', '110101199501011234', 'liming@example.com', 580.0,
     '{"数学":120,"物理":115,"英语":108,"语文":112,"综合":125}'::jsonb, '计算机科学与技术', 'PENDING', 1, NOW() - INTERVAL '5 days', NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, push_round, pushed_at, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_jc_maj_id, 'CAND-002', '王芳', '美国', 'P9876543', 'wangfang@example.com', 520.0,
     '{"数学":98,"物理":85,"英语":130,"语文":105,"综合":102}'::jsonb, '新闻传播学', 'PENDING', 1, NOW() - INTERVAL '3 days', NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, push_round, pushed_at, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-003', '阿里·侯赛因', '巴基斯坦', 'A45678901', 'ali@example.com', 475.0,
     '{"数学":88,"物理":80,"英语":95,"语文":105,"综合":107}'::jsonb, '计算机科学与技术', 'PENDING', 1, NOW() - INTERVAL '1 day', NOW(), NOW(), 0);

    -- ADMITTED: 2人（已录取待确认）
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-004', '朴正恩', '韩国', 'K12345678', 'park@example.com', 598.0,
     '{"数学":128,"物理":122,"英语":118,"语文":108,"综合":122}'::jsonb, '计算机科学与技术', 'ADMITTED', qinghua_cs_maj_id, '恭喜！您的成绩优秀，已被正式录取', 1, NOW() - INTERVAL '10 days', NOW() - INTERVAL '2 days', qinghua_admin_id, NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ir_maj_id, 'CAND-005', '田中太郎', '日本', 'J9876543', 'tanaka@example.com', 540.0,
     '{"数学":105,"物理":98,"英语":135,"语文":102,"综合":100}'::jsonb, '国际关系', 'ADMITTED', qinghua_ir_maj_id, '恭喜！正式录取，请尽快在报名平台确认', 1, NOW() - INTERVAL '8 days', NOW() - INTERVAL '1 day', qinghua_admin_id, NOW(), NOW(), 0);

    -- CONFIRMED: 2人（已确认）
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-006', '玛丽亚·科娃', '俄罗斯', 'R123456789', 'maria@example.com', 565.0,
     '{"数学":118,"物理":112,"英语":125,"语文":100,"综合":110}'::jsonb, '计算机科学与技术', 'CONFIRMED', qinghua_cs_maj_id, '正式录取', 1, NOW() - INTERVAL '15 days', NOW() - INTERVAL '8 days', qinghua_admin_id, NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ee_maj_id, 'CAND-007', '约翰·史密斯', '英国', 'GB123456789', 'john@example.com', 530.0,
     '{"数学":108,"物理":110,"英语":132,"语文":95,"综合":85}'::jsonb, '电子工程', 'CONFIRMED', qinghua_ee_maj_id, '正式录取', 1, NOW() - INTERVAL '12 days', NOW() - INTERVAL '6 days', qinghua_staff_id, NOW(), NOW(), 0);

    -- CHECKED_IN: 1人（已报到）
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-008', '张伟', '中国', '110101199301011234', 'zhangwei@example.com', 590.0,
     '{"数学":125,"物理":120,"英语":110,"语文":115,"综合":120}'::jsonb, '计算机科学与技术', 'CHECKED_IN', qinghua_cs_maj_id, '已报到注册', 1, NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days', qinghua_admin_id, NOW(), NOW(), 0);

    -- CONDITIONAL: 1人（有条件录取）
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, condition_desc, condition_deadline, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_arab_maj_id, 'CAND-009', '阿布杜拉', '沙特阿拉伯', 'SA123456789', 'abdullah@example.com', 490.0,
     '{"数学":92,"物理":85,"英语":140,"语文":78,"综合":95}'::jsonb, '阿拉伯语语言文学', 'CONDITIONAL', qinghua_arab_maj_id,
     '需补充语言水平证明（HSK5级及以上）', NOW() + INTERVAL '10 days', '有条件录取，请在截止日期前补充材料', 1, NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 days', qinghua_admin_id, NOW(), NOW(), 0);

    -- REJECTED: 2人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_maj_id, 'CAND-010', '汤姆·威尔逊', '澳大利亚', 'AUS1234567', 'tom@example.com', 380.0,
     '{"数学":68,"物理":72,"英语":118,"语文":72,"综合":50}'::jsonb, '计算机科学与技术', 'REJECTED', '总成绩未达录取分数线', 1, NOW() - INTERVAL '6 days', NOW() - INTERVAL '4 days', qinghua_admin_id, NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_jc_maj_id, 'CAND-011', '安妮·格林', '加拿大', 'CA9876543', 'anne@example.com', 420.0,
     '{"数学":78,"物理":82,"英语":128,"语文":85,"综合":47}'::jsonb, '新闻传播学', 'REJECTED', '综合成绩偏低，暂不录取', 1, NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days', qinghua_staff_id, NOW(), NOW(), 0);

    -- ===== 北京大学考生 =====

    -- PENDING: 2人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, push_round, pushed_at, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_eco_maj_id, 'CAND-012', '韩梅梅', '中国', '310101199701011234', 'hanmeimei@example.com', 555.0,
     '{"数学":118,"物理":105,"英语":112,"语文":108,"综合":112}'::jsonb, '经济管理', 'PENDING', 1, NOW() - INTERVAL '4 days', NOW(), NOW(), 0);
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, push_round, pushed_at, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_cs_maj_id, 'CAND-013', '金秀贤', '韩国', 'K98765432', 'kim@example.com', 570.0,
     '{"数学":122,"物理":118,"英语":115,"语文":102,"综合":113}'::jsonb, '计算机科学与技术', 'PENDING', 1, NOW() - INTERVAL '2 days', NOW(), NOW(), 0);

    -- ADMITTED: 1人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_law_maj_id, 'CAND-014', '陈思远', '中国', '440101199601012345', 'chensiyuan@example.com', 560.0,
     '{"数学":110,"物理":102,"英语":125,"语文":120,"综合":103}'::jsonb, '法律硕士', 'ADMITTED', pku_law_maj_id, '正式录取，请尽快确认', 1, NOW() - INTERVAL '9 days', NOW() - INTERVAL '1 day', pku_admin_id, NOW(), NOW(), 0);

    -- CONFIRMED: 1人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_major_id, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_eco_maj_id, 'CAND-015', '林晓婷', '中国', '320101199801012345', 'linxiaoting@example.com', 545.0,
     '{"数学":115,"物理":105,"英语":118,"语文":105,"综合":102}'::jsonb, '经济管理', 'CONFIRMED', pku_eco_maj_id, '正式录取', 1, NOW() - INTERVAL '14 days', NOW() - INTERVAL '5 days', pku_admin_id, NOW(), NOW(), 0);

    -- REJECTED: 1人
    INSERT INTO candidate_push (push_id, school_id, major_id, candidate_id, candidate_name, nationality, id_number, email, total_score, subject_scores, intention, status, admission_remark, push_round, pushed_at, operated_at, operator_id, created_at, updated_at, deleted) VALUES
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_ir_maj_id, 'CAND-016', '大卫·米勒', '美国', 'USA1234567', 'david@example.com', 400.0,
     '{"数学":72,"物理":68,"英语":140,"语文":70,"综合":50}'::jsonb, '国际关系', 'REJECTED', '专业背景不符合要求', 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days', pku_admin_id, NOW(), NOW(), 0);

END;
$$;

-- ============================================================
-- Step 4: 添加分数线
-- ============================================================

DO $$
DECLARE
    qinghua_cs_u_id uuid;
    qinghua_ir_id uuid;
    qinghua_arab_id uuid;
    pku_cs_u_id uuid;
    pku_law_id uuid;
BEGIN
    SELECT major_id INTO qinghua_cs_u_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO qinghua_ir_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '国际关系' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_arab_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '阿拉伯语语言文学' AND degree_level = '硕士';
    SELECT major_id INTO pku_cs_u_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO pku_law_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '法律硕士' AND degree_level = '硕士';

    INSERT INTO score_line (score_line_id, school_id, major_id, year, subject, min_score, created_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_u_id, 2026, '总分', 450, NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_cs_u_id, 2026, '数学', 100, NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ir_id, 2026, '总分', 420, NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_ir_id, 2026, '英语', 120, NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_arab_id, 2026, '总分', 400, NOW(), 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', qinghua_arab_id, 2026, '英语', 130, NOW(), 0),
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_cs_u_id, 2026, '总分', 460, NOW(), 0),
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_cs_u_id, 2026, '数学', 105, NOW(), 0),
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_law_id, 2026, '总分', 430, NOW(), 0),
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', pku_law_id, 2026, '语文', 100, NOW(), 0);
END;
$$;

-- ============================================================
-- Step 5: 添加操作日志（录取、确认、报到等）
-- ============================================================

DO $$
DECLARE
    qinghua_cs_id uuid;
    qinghua_ir_id uuid;
    qinghua_ee_id uuid;
    qinghua_arab_id uuid;
    qinghua_cs_u_id uuid;
    qinghua_admin_id uuid;
    qinghua_staff_id uuid;
    pku_law_id uuid;
    pku_eco_id uuid;
    pku_admin_id uuid;
    p1 uuid; p2 uuid; p3 uuid; p4 uuid; p5 uuid; p6 uuid; p7 uuid; p8 uuid;
    c1 uuid; c2 uuid;
BEGIN
    SELECT major_id INTO qinghua_cs_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT major_id INTO qinghua_ir_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '国际关系' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_ee_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '电子工程' AND degree_level = '本科';
    SELECT major_id INTO qinghua_arab_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '阿拉伯语语言文学' AND degree_level = '硕士';
    SELECT major_id INTO qinghua_cs_u_id FROM major WHERE school_id = '94249003-5e2f-40a2-be75-ee053083fbf7' AND major_name = '计算机科学与技术' AND degree_level = '本科';
    SELECT account_id INTO qinghua_admin_id FROM account WHERE username = 'school_admin2';
    SELECT account_id INTO qinghua_staff_id FROM account WHERE username = 'test_staff_001';
    SELECT major_id INTO pku_law_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '法律硕士' AND degree_level = '硕士';
    SELECT major_id INTO pku_eco_id FROM major WHERE school_id = '2eaed362-3f57-49a6-b1b1-3160b15b9199' AND major_name = '经济管理' AND degree_level = '本科';
    SELECT account_id INTO pku_admin_id FROM account WHERE username = 'op_admin';

    -- 找push_id
    SELECT push_id INTO p1 FROM candidate_push WHERE candidate_id = 'CAND-004';
    SELECT push_id INTO p2 FROM candidate_push WHERE candidate_id = 'CAND-005';
    SELECT push_id INTO p3 FROM candidate_push WHERE candidate_id = 'CAND-006';
    SELECT push_id INTO p4 FROM candidate_push WHERE candidate_id = 'CAND-007';
    SELECT push_id INTO p5 FROM candidate_push WHERE candidate_id = 'CAND-008';
    SELECT push_id INTO p6 FROM candidate_push WHERE candidate_id = 'CAND-009';
    SELECT push_id INTO p7 FROM candidate_push WHERE candidate_id = 'CAND-010';
    SELECT push_id INTO p8 FROM candidate_push WHERE candidate_id = 'CAND-011';
    SELECT push_id INTO c1 FROM candidate_push WHERE candidate_id = 'CAND-014';
    SELECT push_id INTO c2 FROM candidate_push WHERE candidate_id = 'CAND-015';

    INSERT INTO operation_log (log_id, push_id, action, operator_id, operator_name, remark, created_at) VALUES
    (gen_random_uuid(), p4, 'ADMIT', qinghua_admin_id, '院校管理员B', '正式录取', NOW() - INTERVAL '2 days'),
    (gen_random_uuid(), p2, 'ADMIT', qinghua_admin_id, '院校管理员B', '正式录取', NOW() - INTERVAL '1 day'),
    (gen_random_uuid(), p1, 'FINALIZE', qinghua_admin_id, '院校管理员B', '终裁录取', NOW() - INTERVAL '2 days'),
    (gen_random_uuid(), p3, 'FINALIZE', qinghua_admin_id, '院校管理员B', '终裁录取', NOW() - INTERVAL '8 days'),
    (gen_random_uuid(), p4, 'CONFIRM', qinghua_admin_id, '院校管理员B', '考生已确认录取', NOW() - INTERVAL '7 days'),
    (gen_random_uuid(), p3, 'CONFIRM', qinghua_admin_id, '院校管理员B', '考生已确认录取', NOW() - INTERVAL '5 days'),
    (gen_random_uuid(), p5, 'MATERIAL_RECEIVE', qinghua_admin_id, '院校管理员B', '材料已收', NOW() - INTERVAL '10 days'),
    (gen_random_uuid(), p5, 'CHECKIN', qinghua_admin_id, '院校管理员B', '报到确认完成', NOW() - INTERVAL '10 days'),
    (gen_random_uuid(), p6, 'CONDITIONAL', qinghua_admin_id, '院校管理员B', '有条件录取：需补充HSK5级证明', NOW() - INTERVAL '2 days'),
    (gen_random_uuid(), p7, 'REJECT', qinghua_admin_id, '院校管理员B', '总成绩未达录取分数线', NOW() - INTERVAL '4 days'),
    (gen_random_uuid(), p8, 'REJECT', qinghua_staff_id, '测试工作人员', '综合成绩偏低', NOW() - INTERVAL '2 days'),
    (gen_random_uuid(), c1, 'ADMIT', pku_admin_id, '系统管理员', '正式录取', NOW() - INTERVAL '1 day'),
    (gen_random_uuid(), c2, 'FINALIZE', pku_admin_id, '系统管理员', '终裁录取', NOW() - INTERVAL '5 days'),
    (gen_random_uuid(), c2, 'CONFIRM', pku_admin_id, '系统管理员', '考生已确认录取', NOW() - INTERVAL '4 days');
END;
$$;

-- ============================================================
-- Step 6: 添加站内通知
-- ============================================================

DO $$
DECLARE
    qinghua_admin_id uuid;
    pku_admin_id uuid;
    p1 uuid; p2 uuid; p3 uuid; p4 uuid; p5 uuid; p6 uuid; c1 uuid;
BEGIN
    SELECT account_id INTO qinghua_admin_id FROM account WHERE username = 'school_admin2';
    SELECT account_id INTO pku_admin_id FROM account WHERE username = 'op_admin';
    SELECT push_id INTO p1 FROM candidate_push WHERE candidate_id = 'CAND-004';
    SELECT push_id INTO p2 FROM candidate_push WHERE candidate_id = 'CAND-005';
    SELECT push_id INTO p3 FROM candidate_push WHERE candidate_id = 'CAND-006';
    SELECT push_id INTO p4 FROM candidate_push WHERE candidate_id = 'CAND-007';
    SELECT push_id INTO p5 FROM candidate_push WHERE candidate_id = 'CAND-008';
    SELECT push_id INTO p6 FROM candidate_push WHERE candidate_id = 'CAND-009';
    SELECT push_id INTO c1 FROM candidate_push WHERE candidate_id = 'CAND-014';

    INSERT INTO notification (notification_id, school_id, push_id, recipient_id, title, content, is_read, created_at, deleted) VALUES
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p1, qinghua_admin_id, '考生朴正恩已被正式录取', '考生朴正恩（计算机科学与技术，总分598）已被正式录取，等待考生确认。', false, NOW() - INTERVAL '2 days', 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p2, qinghua_admin_id, '考生田中太郎已被正式录取', '考生田中太郎（国际关系，总分540）已被正式录取，等待考生确认。', false, NOW() - INTERVAL '1 day', 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p3, qinghua_admin_id, '考生玛丽亚·科娃已确认录取', '考生玛丽亚·科娃已确认录取，请准备材料收取事宜。', false, NOW() - INTERVAL '7 days', 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p4, qinghua_admin_id, '考生约翰·史密斯已确认录取', '考生约翰·史密斯已确认录取，请准备材料收取事宜。', false, NOW() - INTERVAL '6 days', 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p5, qinghua_admin_id, '考生张伟已完成报到', '考生张伟已完成报到注册。', false, NOW() - INTERVAL '10 days', 0),
    (gen_random_uuid(), '94249003-5e2f-40a2-be75-ee053083fbf7', p6, qinghua_admin_id, '有条件录取待处理', '考生阿布杜拉的有条件录取（HSK5级证明）将在10天后到期，请及时跟进。', true, NOW() - INTERVAL '2 days', 0),
    (gen_random_uuid(), '2eaed362-3f57-49a6-b1b1-3160b15b9199', c1, pku_admin_id, '考生陈思远已被正式录取', '考生陈思远（法律硕士，总分560）已被正式录取，等待考生确认。', false, NOW() - INTERVAL '1 day', 0);
END;
$$;

-- ============================================================
-- 验证结果
-- ============================================================

SELECT '院校' as tbl, COUNT(*) as cnt FROM school WHERE deleted = 0
UNION ALL SELECT '专业', COUNT(*) FROM major WHERE deleted = 0
UNION ALL SELECT '名额', COUNT(*) FROM admission_quota WHERE deleted = 0
UNION ALL SELECT '考生', COUNT(*) FROM candidate_push WHERE deleted = 0
UNION ALL SELECT '分数线', COUNT(*) FROM score_line WHERE deleted = 0
UNION ALL SELECT '操作日志', COUNT(*) FROM operation_log
UNION ALL SELECT '通知', COUNT(*) FROM notification WHERE deleted = 0
;
