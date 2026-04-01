-- V6: 新增 ENROLLED_ELSEWHERE（已被他校录取）状态
-- 场景：考生成绩推送给多所学校，在某校确认录取后，其他学校中尚处于 PENDING/CONDITIONAL 的记录自动标记

ALTER TABLE candidate_push DROP CONSTRAINT IF EXISTS chk_push_status;
ALTER TABLE candidate_push ADD CONSTRAINT chk_push_status CHECK (status IN (
    'PENDING', 'CONDITIONAL', 'ADMITTED', 'CONFIRMED',
    'MATERIAL_RECEIVED', 'CHECKED_IN', 'REJECTED', 'INVALIDATED',
    'ENROLLED_ELSEWHERE'
));
