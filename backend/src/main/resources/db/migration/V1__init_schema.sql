-- =========================================================
-- 院校管理平台 V1__init_schema.sql
-- 创建全部 16 张表、索引、外键约束、CHECK 枚举约束
-- 基于：数据库设计文档 V1.0
-- =========================================================

-- 1. school（院校信息表）
CREATE TABLE school (
    school_id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name        VARCHAR(100) NOT NULL,
    school_short_name  VARCHAR(20)  NOT NULL,
    province           VARCHAR(50)  NOT NULL,
    school_type        VARCHAR(20)  NOT NULL,
    contact_name       VARCHAR(50)  NOT NULL,
    contact_phone      VARCHAR(11)  NOT NULL,
    contact_email      VARCHAR(200) NOT NULL,
    website            VARCHAR(500),
    remark             VARCHAR(500),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         UUID,

    CONSTRAINT chk_school_status  CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_school_type    CHECK (school_type IN ('综合类', '理工类', '文史类', '艺术类', '其他')),
    CONSTRAINT chk_contact_phone  CHECK (contact_phone ~ '^1[3-9]\d{9}$')
);

CREATE UNIQUE INDEX uniq_school_name      ON school (LOWER(school_name));
CREATE INDEX idx_school_province          ON school (province);
CREATE INDEX idx_school_status            ON school (status);
CREATE INDEX idx_school_type              ON school (school_type);

-- 2. account（账号表）
CREATE TABLE account (
    account_id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username             VARCHAR(30) NOT NULL,
    password_hash        VARCHAR(200) NOT NULL,
    role                 VARCHAR(20) NOT NULL,
    school_id            UUID,
    real_name            VARCHAR(50) NOT NULL,
    phone                VARCHAR(11),
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_count   INTEGER     NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    must_change_password BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           UUID,

    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    CONSTRAINT chk_account_role   CHECK (role IN ('OP_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_STAFF')),
    CONSTRAINT fk_account_school   FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uniq_account_username ON account (LOWER(username));
CREATE UNIQUE INDEX uniq_school_admin ON account (school_id)
    WHERE role = 'SCHOOL_ADMIN' AND status != 'INACTIVE';
CREATE INDEX idx_account_school   ON account (school_id);
CREATE INDEX idx_account_role     ON account (role);
CREATE INDEX idx_account_status   ON account (status);

-- 3. major（专业表）
CREATE TABLE major (
    major_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID         NOT NULL,
    major_name    VARCHAR(100) NOT NULL,
    degree_level  VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_major_school   FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE CASCADE,
    CONSTRAINT chk_major_status  CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_major_degree  CHECK (degree_level IN ('本科', '硕士', '博士'))
);

CREATE UNIQUE INDEX uniq_major_school_name ON major (school_id, major_name);
CREATE INDEX idx_major_school  ON major (school_id);
CREATE INDEX idx_major_status  ON major (status);

-- 4. admission_quota（招生名额表，含乐观锁 version）
CREATE TABLE admission_quota (
    quota_id        UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID       NOT NULL,
    major_id        UUID       NOT NULL,
    year             INTEGER    NOT NULL DEFAULT EXTRACT(YEAR FROM NOW()),
    total_quota      INTEGER    NOT NULL CHECK (total_quota >= 0),
    admitted_count   INTEGER    NOT NULL DEFAULT 0 CHECK (admitted_count >= 0),
    reserved_count   INTEGER    NOT NULL DEFAULT 0 CHECK (reserved_count >= 0),
    version          INTEGER    NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quota_school FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE CASCADE,
    CONSTRAINT fk_quota_major  FOREIGN KEY (major_id)  REFERENCES major(major_id)   ON DELETE CASCADE,
    CONSTRAINT chk_quota_range CHECK (admitted_count + reserved_count <= total_quota)
);

CREATE UNIQUE INDEX uniq_quota_school_major_year ON admission_quota (school_id, major_id, year);
CREATE INDEX idx_quota_school  ON admission_quota (school_id);
CREATE INDEX idx_quota_major   ON admission_quota (major_id);
CREATE INDEX idx_quota_year    ON admission_quota (year);

-- 5. score_line（分数线表）
CREATE TABLE score_line (
    line_id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID        NOT NULL,
    major_id     UUID        NOT NULL,
    year          INTEGER     NOT NULL DEFAULT EXTRACT(YEAR FROM NOW()),
    subject       VARCHAR(50) NOT NULL,
    min_score     DECIMAL(5,1) NOT NULL CHECK (min_score >= 0 AND min_score <= 100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_line_school FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE CASCADE,
    CONSTRAINT fk_line_major  FOREIGN KEY (major_id)  REFERENCES major(major_id)   ON DELETE CASCADE
);

CREATE UNIQUE INDEX uniq_line_school_major_year_subject ON score_line (school_id, major_id, year, subject);
CREATE INDEX idx_line_school  ON score_line (school_id);
CREATE INDEX idx_line_major   ON score_line (major_id);

-- 6. school_config（院校 KV 配置表）
CREATE TABLE school_config (
    config_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID,
    config_key  VARCHAR(100) NOT NULL,
    config_value TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_config_school FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uniq_config_school_key ON school_config (school_id, config_key);
CREATE INDEX idx_config_school  ON school_config (school_id);

-- 7. school_brochure（招生简章表）
CREATE TABLE school_brochure (
    brochure_id  UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID       NOT NULL UNIQUE,
    title        VARCHAR(200) NOT NULL,
    content      TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_brochure_school FOREIGN KEY (school_id) REFERENCES school(school_id) ON DELETE CASCADE,
    CONSTRAINT chk_brochure_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_brochure_school ON school_brochure (school_id);

-- 8. candidate_push（考生推送记录，核心业务表）
CREATE TABLE candidate_push (
    push_id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID        NOT NULL,
    major_id          UUID,
    candidate_id      VARCHAR(50) NOT NULL,
    candidate_name    VARCHAR(100) NOT NULL,
    nationality       VARCHAR(50),
    id_number         VARCHAR(50),
    email             VARCHAR(200),
    total_score       DECIMAL(6,1),
    subject_scores    JSONB,
    intention         TEXT,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    admission_major_id UUID,
    admission_remark  TEXT,
    condition_desc    TEXT,
    condition_deadline TIMESTAMPTZ,
    push_round         INTEGER     NOT NULL DEFAULT 0,
    pushed_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    operated_at       TIMESTAMPTZ,
    operator_id       UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_push_school FOREIGN KEY (school_id)        REFERENCES school(school_id) ON DELETE CASCADE,
    CONSTRAINT fk_push_major  FOREIGN KEY (major_id)         REFERENCES major(major_id)   ON DELETE SET NULL,
    CONSTRAINT fk_push_adm_major FOREIGN KEY (admission_major_id) REFERENCES major(major_id) ON DELETE SET NULL,
    CONSTRAINT fk_push_operator  FOREIGN KEY (operator_id)   REFERENCES account(account_id) ON DELETE SET NULL,
    CONSTRAINT chk_push_status  CHECK (status IN (
        'PENDING', 'CONDITIONAL', 'ADMITTED', 'CONFIRMED',
        'MATERIAL_RECEIVED', 'CHECKED_IN', 'REJECTED', 'INVALIDATED'
    ))
);

CREATE UNIQUE INDEX uniq_push_school_candidate ON candidate_push (school_id, candidate_id);
CREATE INDEX idx_push_school       ON candidate_push (school_id);
CREATE INDEX idx_push_candidate_id  ON candidate_push (candidate_id);
CREATE INDEX idx_push_status        ON candidate_push (status);
CREATE INDEX idx_push_major         ON candidate_push (major_id);
CREATE INDEX idx_push_pushed_at     ON candidate_push (pushed_at);
CREATE INDEX idx_push_condition_deadline ON candidate_push (condition_deadline)
    WHERE condition_deadline IS NOT NULL AND status = 'CONDITIONAL';

-- 9. admission_log（录取操作日志）
CREATE TABLE admission_log (
    log_id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    push_id        UUID        NOT NULL,
    school_id      UUID        NOT NULL,
    major_id       UUID,
    operator_id    UUID,
    action         VARCHAR(50) NOT NULL,
    before_status  VARCHAR(30),
    after_status   VARCHAR(30) NOT NULL,
    remark         TEXT,
    ip_address     VARCHAR(50),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_log_push    FOREIGN KEY (push_id)    REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    CONSTRAINT fk_log_school  FOREIGN KEY (school_id)  REFERENCES school(school_id)     ON DELETE CASCADE,
    CONSTRAINT fk_log_major   FOREIGN KEY (major_id)   REFERENCES major(major_id)        ON DELETE SET NULL,
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES account(account_id)  ON DELETE SET NULL
);

CREATE INDEX idx_log_push_id    ON admission_log (push_id);
CREATE INDEX idx_log_school_id  ON admission_log (school_id);
CREATE INDEX idx_log_operator_id ON admission_log (operator_id);
CREATE INDEX idx_log_created_at ON admission_log (created_at);

-- 10. supplement_round（补录轮次表）
CREATE TABLE supplement_round (
    round_id       UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    round_number   INTEGER    NOT NULL,
    start_time     TIMESTAMPTZ NOT NULL,
    end_time       TIMESTAMPTZ NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    remark         TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_round_status CHECK (status IN ('UPCOMING', 'ACTIVE', 'CLOSED')),
    CONSTRAINT chk_round_time   CHECK (end_time > start_time)
);

CREATE INDEX idx_round_status    ON supplement_round (status);
CREATE INDEX idx_round_time      ON supplement_round (start_time, end_time);

-- 11. verification_log（成绩核验表）
CREATE TABLE verification_log (
    verification_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL,
    push_id         UUID        NOT NULL,
    operator_id     UUID        NOT NULL,
    action          VARCHAR(50) NOT NULL,
    certificate_no  VARCHAR(100),
    result          VARCHAR(20) NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_verif_school   FOREIGN KEY (school_id)   REFERENCES school(school_id)      ON DELETE CASCADE,
    CONSTRAINT fk_verif_push     FOREIGN KEY (push_id)    REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    CONSTRAINT fk_verif_operator FOREIGN KEY (operator_id) REFERENCES account(account_id)    ON DELETE CASCADE,
    CONSTRAINT chk_verif_result  CHECK (result IN ('PASSED', 'FAILED', 'PENDING'))
);

CREATE INDEX idx_verif_school   ON verification_log (school_id);
CREATE INDEX idx_verif_push     ON verification_log (push_id);
CREATE INDEX idx_verif_operator ON verification_log (operator_id);
CREATE INDEX idx_verif_created  ON verification_log (created_at);

-- 12. material_receive_log（材料收件表）
CREATE TABLE material_receive_log (
    receive_id      UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
    push_id         UUID      NOT NULL UNIQUE,
    operator_id     UUID      NOT NULL,
    receive_time    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tracking_no     VARCHAR(100),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_receive_push    FOREIGN KEY (push_id)    REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    CONSTRAINT fk_receive_operator FOREIGN KEY (operator_id) REFERENCES account(account_id)    ON DELETE CASCADE
);

CREATE INDEX idx_receive_push_id    ON material_receive_log (push_id);
CREATE INDEX idx_receive_operator   ON material_receive_log (operator_id);

-- 13. checkin_log（报到确认表）
CREATE TABLE checkin_log (
    checkin_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    push_id      UUID       NOT NULL UNIQUE,
    operator_id  UUID       NOT NULL,
    checkin_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_checkin_push    FOREIGN KEY (push_id)    REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    CONSTRAINT fk_checkin_operator FOREIGN KEY (operator_id) REFERENCES account(account_id)    ON DELETE CASCADE
);

CREATE INDEX idx_checkin_push_id    ON checkin_log (push_id);
CREATE INDEX idx_checkin_operator   ON checkin_log (operator_id);

-- 14. notification（站内通知表）
CREATE TABLE notification (
    notification_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,
    push_id         UUID,
    recipient_id    UUID       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    content         TEXT,
    is_read         BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notif_school  FOREIGN KEY (school_id) REFERENCES school(school_id)      ON DELETE CASCADE,
    CONSTRAINT fk_notif_push    FOREIGN KEY (push_id)   REFERENCES candidate_push(push_id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES account(account_id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_recipient ON notification (recipient_id);
CREATE INDEX idx_notif_school    ON notification (school_id);
CREATE INDEX idx_notif_is_read  ON notification (is_read);
CREATE INDEX idx_notif_created  ON notification (created_at DESC);

-- 15. audit_log（审计日志表）
CREATE TABLE audit_log (
    audit_id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    operator_id      UUID,
    operator_role    VARCHAR(20),
    school_id        UUID,
    action           VARCHAR(100) NOT NULL,
    target_type      VARCHAR(50),
    target_id        UUID,
    before_snapshot  JSONB,
    after_snapshot   JSONB,
    ip_address       VARCHAR(50),
    user_agent       VARCHAR(500),
    operated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (audit_id, operated_at)
)
PARTITION BY RANGE (operated_at);

-- 按月分区（生产环境建议提前创建未来分区）
CREATE TABLE audit_log_2026_01 PARTITION OF audit_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_log_2026_02 PARTITION OF audit_log
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE audit_log_2026_03 PARTITION OF audit_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE audit_log_2026_04 PARTITION OF audit_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE audit_log_2026_05 PARTITION OF audit_log
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE audit_log_2026_06 PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE INDEX idx_audit_operator  ON audit_log (operator_id);
CREATE INDEX idx_audit_school    ON audit_log (school_id);
CREATE INDEX idx_audit_action    ON audit_log (action);
CREATE INDEX idx_audit_operated  ON audit_log (operated_at DESC);

-- 16. export_task（导出任务表）
CREATE TABLE export_task (
    task_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    operator_id    UUID        NOT NULL,
    school_id      UUID,
    task_type      VARCHAR(50) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_path      VARCHAR(500),
    file_key       VARCHAR(200),
    query_params   JSONB,
    total_count    INTEGER,
    exported_count INTEGER,
    error_message  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at    TIMESTAMPTZ,

    CONSTRAINT fk_export_operator FOREIGN KEY (operator_id) REFERENCES account(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_export_school    FOREIGN KEY (school_id)   REFERENCES school(school_id)   ON DELETE SET NULL,
    CONSTRAINT chk_export_status   CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_export_operator  ON export_task (operator_id);
CREATE INDEX idx_export_school   ON export_task (school_id);
CREATE INDEX idx_export_status   ON export_task (status);
CREATE INDEX idx_export_created  ON export_task (created_at DESC);

-- =========================================================
-- 全局系统参数初始数据
-- =========================================================
INSERT INTO school_config (school_id, config_key, config_value) VALUES
    (NULL, 'max_staff_per_school',    '50'),
    (NULL, 'default_admission_deadline', ''),
    (NULL, 'session_validity_hours',  '8'),
    (NULL, 'login_max_attempts',       '5'),
    (NULL, 'lock_duration_minutes',   '30'),
    (NULL, 'sms_code_ttl_seconds',    '300'),
    (NULL, 'sms_daily_limit',         '5');

-- =========================================================
-- 系统初始账号（运营管理员，系统管理员由开发团队直接写入）
-- 用户名: op_admin，密码: OpAdmin@2026（仅演示用，生产必须修改）
-- =========================================================
INSERT INTO account (account_id, username, password_hash, role, school_id, real_name, phone, status, must_change_password)
VALUES (
    gen_random_uuid(),
    'op_admin',
    '$2b$12$4on5s2/QZ/UZnZY8YgOSmOzbuk.BxaQxhcy/OR5MG7/Z7KqpaBFGS', -- OpAdmin@2026
    'OP_ADMIN',
    NULL,
    '系统管理员',
    '13800000000',
    'ACTIVE',
    TRUE
);

COMMENT ON TABLE school              IS '院校信息表，系统最顶层组织实体';
COMMENT ON TABLE account             IS '账号表，支持 OP_ADMIN / SCHOOL_ADMIN / SCHOOL_STAFF 三种角色';
COMMENT ON TABLE major               IS '专业表，专业名称在同一院校内唯一';
COMMENT ON TABLE admission_quota      IS '招生名额表，version 字段用于乐观锁并发控制';
COMMENT ON TABLE score_line           IS '分数线表，按院校/专业/年份/科目配置';
COMMENT ON TABLE school_config        IS '院校 KV 配置表（如录取截止时间等）';
COMMENT ON TABLE school_brochure      IS '院校招生简章（富文本）';
COMMENT ON TABLE candidate_push      IS '考生推送记录，核心业务表，记录完整招录状态机';
COMMENT ON TABLE admission_log       IS '录取操作日志，完整操作链路追溯';
COMMENT ON TABLE supplement_round    IS '补录轮次配置';
COMMENT ON TABLE verification_log    IS '成绩证书核验记录';
COMMENT ON TABLE material_receive_log IS '材料收件登记记录';
COMMENT ON TABLE checkin_log         IS '报到确认记录';
COMMENT ON TABLE notification         IS '站内通知消息';
COMMENT ON TABLE audit_log           IS '全局审计日志（按月分区）';
COMMENT ON TABLE export_task          IS '导出任务管理';
