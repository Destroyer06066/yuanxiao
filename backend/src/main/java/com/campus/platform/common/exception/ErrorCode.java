package com.campus.platform.common.exception;

/**
 * 错误码规范：
 * 1xxxx - 账号/权限相关
 * 2xxxx - 业务数据相关（院校、专业、名额等）
 * 3xxxx - 录取操作相关
 * 4xxxx - 外部接口相关
 * 5xxxx - 系统/服务端错误
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ========== 账号/权限 1xxxx ==========
    public static final Integer USERNAME_OR_PASSWORD_ERROR  = 10001;
    public static final Integer ACCOUNT_DISABLED             = 10002;
    public static final Integer ACCOUNT_LOCKED               = 10003;
    public static final Integer SCHOOL_DISABLED             = 10004;
    public static final Integer USERNAME_ALREADY_EXISTS      = 10005;
    public static final Integer PHONE_NOT_BOUND             = 10006;
    public static final Integer SMS_RATE_LIMIT              = 10007;
    public static final Integer SMS_CODE_ERROR              = 10008;
    public static final Integer SMS_CODE_EXPIRED            = 10009;
    public static final Integer PASSWORD_POLICY_VIOLATION   = 10010;
    public static final Integer PASSWORD_SAME_AS_OLD        = 10011;
    public static final Integer UNAUTHORIZED                = 10012;
    public static final Integer FORBIDDEN                   = 10013;
    public static final Integer STAFF_QUOTA_EXCEEDED        = 10014;

    // ========== 业务数据 2xxxx ==========
    public static final Integer SCHOOL_NOT_FOUND            = 20001;
    public static final Integer SCHOOL_NAME_DUPLICATE       = 20002;
    public static final Integer SCHOOL_ADMIN_EXISTS         = 20003;
    public static final Integer ACCOUNT_NOT_FOUND           = 20004;
    public static final Integer MAJOR_NOT_FOUND             = 20005;
    public static final Integer MAJOR_NAME_DUPLICATE       = 20006;
    public static final Integer QUOTA_NOT_FOUND             = 20007;
    public static final Integer QUOTA_NOT_ENOUGH           = 20008;
    public static final Integer QUOTA_CONCURRENT_CONFLICT   = 20009;
    public static final Integer ROUND_NOT_FOUND             = 20010;
    public static final Integer ROUND_NOT_ACTIVE           = 20011;

    // ========== 录取操作 3xxxx ==========
    public static final Integer CANDIDATE_NOT_FOUND        = 30001;
    public static final Integer STATUS_TRANSITION_INVALID   = 30002;
    public static final Integer ADMISSION_DEADLINE_PASSED   = 30003;
    public static final Integer DUPLICATE_ADMISSION         = 30004;
    public static final Integer ADMISSION_REVOKE_DEADLINE   = 30005;
    public static final Integer CONDITION_NOT_MET           = 30006;
    public static final Integer ALREADY_TERMINAL_STATE      = 30007;
    public static final Integer VERIFICATION_NOT_FOUND      = 30008;
    public static final Integer MATERIAL_ALREADY_RECEIVED  = 30009;
    public static final Integer CHECKIN_ALREADY_DONE        = 30010;

    // ========== 外部接口 4xxxx ==========
    public static final Integer INTEGRATION_PUSH_ERROR      = 40001;
    public static final Integer INTEGRATION_NOTIFY_ERROR    = 40002;

    // ========== 系统/服务端 5xxxx ==========
    public static final Integer INTERNAL_ERROR              = 50001;
    public static final Integer INVALID_PARAMETER            = 50002;
    public static final Integer RESOURCE_NOT_FOUND          = 50003;
    public static final Integer FILE_UPLOAD_ERROR          = 50004;
    public static final Integer EXPORT_TASK_ERROR           = 50005;
}
