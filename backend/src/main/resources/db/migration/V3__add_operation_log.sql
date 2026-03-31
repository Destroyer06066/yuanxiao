-- 操作日志表（考生状态变更时间线）
CREATE TABLE IF NOT EXISTS operation_log (
    log_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    push_id        UUID NOT NULL REFERENCES candidate_push(push_id),
    action         VARCHAR(50) NOT NULL,
    operator_id    UUID,
    operator_name  VARCHAR(100),
    remark         TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_oplog_push_id ON operation_log(push_id);
CREATE INDEX IF NOT EXISTS idx_oplog_created ON operation_log(created_at DESC);

COMMENT ON TABLE operation_log IS '考生操作日志，记录每次状态变更';
COMMENT ON COLUMN operation_log.action IS '操作类型: PUSH, ADMIT, CONDITIONAL, FINALIZE, REJECT, CONFIRM, MATERIAL_RECEIVE, CHECKIN, INVALIDATE';
