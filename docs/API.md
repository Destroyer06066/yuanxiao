# 院校管理平台 — API 接口文档

> 本文档描述院校管理平台后端全部 RESTful API 接口。启动应用后可通过 Swagger UI 查看交互式文档。

## 认证

所有 API（除登录相关）需要在请求头中携带 JWT Token：

```
Authorization: Bearer <accessToken>
```

响应中若出现 `X-New-Token` 头，请替换本地存储的 Token。

---

## 认证模块 `/api/v1/auth`

### 登录
```
POST /api/v1/auth/login
Body: { "username": "string", "password": "string" }
Response: { "code": 0, "data": { "accessToken": "...", "role": "...", "realName": "...", "requirePasswordChange": true/false } }
```

### 登出
```
POST /api/v1/auth/logout
Response: { "code": 0 }
```

### 获取当前用户信息
```
GET /api/v1/auth/me
Response: { "code": 0, "data": { "accountId": "uuid", "username": "...", "role": "...", "schoolId": "uuid|null", "realName": "..." } }
```

### 强制修改密码（首次登录）
```
PUT /api/v1/auth/password/force-change
Body: { "newPassword": "string" }
Response: { "code": 0 }
```

### 忘记密码 — 发送验证码
```
POST /api/v1/auth/password/reset/send-code
Body: { "username": "string" }
Response: { "code": 0 }
```

### 忘记密码 — 验证并重置
```
POST /api/v1/auth/password/reset/confirm
Body: { "username": "string", "code": "string", "newPassword": "string" }
Response: { "code": 0 }
```

---

## 院校管理 `/api/v1/admin/schools`

### 创建院校（OP_ADMIN）
```
POST /api/v1/admin/schools
Body: { "schoolName": "string", "schoolShortName": "string", "province": "string", "schoolType": "string", "contactName": "string", "contactPhone": "string", "contactEmail": "string", "website": "string?", "remark": "string?" }
Response: { "code": 0, "data": { "schoolId": "uuid" } }
```

### 查询院校列表（OP_ADMIN）
```
GET /api/v1/admin/schools
Query: keyword, province, schoolType, status, page, pageSize
Response: { "code": 0, "data": { "records": [...], "total": N, "page": 1, "pageSize": 20 } }
```

### 编辑院校（OP_ADMIN）
```
PUT /api/v1/admin/schools/{schoolId}
Body: { ... 同创建字段 }
Response: { "code": 0 }
```

### 停用/启用院校（OP_ADMIN）
```
PATCH /api/v1/admin/schools/{schoolId}/status
Body: { "status": "ACTIVE" | "INACTIVE" }
Response: { "code": 0 }
```

---

## 账号管理 `/api/v1`

### 创建院校管理员账号（OP_ADMIN）
```
POST /api/v1/admin/schools/{schoolId}/admin-account
Body: { "username": "string", "realName": "string", "phone": "string?" }
Response: { "code": 0, "data": { "accountId": "uuid", "username": "string", "initialPassword": "string" } }
```

### 创建工作人员账号（SCHOOL_ADMIN）
```
POST /api/v1/schools/{schoolId}/staff-accounts
Body: { "username": "string", "realName": "string", "phone": "string?", "initialPassword": "string?" }
Response: { "code": 0, "data": { "accountId": "uuid", "username": "string", "initialPassword": "string" } }
```

### 停用/启用账号
```
PATCH /api/v1/accounts/{accountId}/status
Body: { "status": "ACTIVE" | "INACTIVE" }
Response: { "code": 0 }
```

### 重置密码
```
POST /api/v1/accounts/{accountId}/reset-password
Response: { "code": 0, "data": { "newPassword": "string" } }
```

---

## 考生管理 `/api/v1/students`

### 查询考生列表
```
GET /api/v1/students
Query: status[], minScore, maxScore, intentionKeyword, nationality, pushTimeStart, pushTimeEnd, majorId, round, sort, order, page, pageSize
Response: { "code": 0, "data": { "records": [...], "total": N, "page": 1, "pageSize": 20 } }
```

### 考生详情
```
GET /api/v1/students/{pushId}
Response: { "code": 0, "data": { ... } }
```

---

## 录取操作 `/api/v1/admissions`

### 直接录取
```
POST /api/v1/admissions/direct
Body: { "pushId": "uuid", "majorId": "uuid", "remark": "string?" }
Response: { "code": 0 }
```

### 有条件录取
```
POST /api/v1/admissions/conditional
Body: { "pushId": "uuid", "majorId": "uuid", "conditionDesc": "string", "conditionDeadline": "YYYY-MM-DD" }
Response: { "code": 0 }
```

### 终裁录取（条件满足后）
```
POST /api/v1/admissions/final/{pushId}
Response: { "code": 0 }
```

### 撤销录取（截止时间前）
```
POST /api/v1/admissions/revoke/{pushId}
Response: { "code": 0 }
```

### 批量拒绝
```
POST /api/v1/admissions/batch-reject
Body: { "pushIds": ["uuid"] }
Response: { "code": 0 }
```

---

## 专业配置 `/api/v1/majors`

```
GET    /api/v1/majors              # 查询本校专业列表
POST   /api/v1/majors              # 创建专业（SCHOOL_ADMIN）
PUT    /api/v1/majors/{majorId}    # 编辑专业
DELETE /api/v1/majors/{majorId}     # 删除专业
```

---

## 名额管理 `/api/v1/quota`

```
GET    /api/v1/quota                      # 查询本校名额
POST   /api/v1/quota                       # 创建/更新名额
```

---

## 补录管理 `/api/v1/supplement`

```
GET    /api/v1/supplement/rounds           # 查询补录轮次
POST   /api/v1/supplement/rounds           # 创建轮次（OP_ADMIN）
PATCH  /api/v1/supplement/rounds/{roundId} # 更新轮次状态
```

---

## 站内通知 `/api/v1/notifications`

```
GET    /api/v1/notifications               # 查询通知列表（分页）
PATCH  /api/v1/notifications/{id}/read     # 标为已读
POST   /api/v1/notifications/read-all     # 全部标为已读
```

---

## 报名平台集成 `/api/v1/integration`（由报名平台调用）

### 推送考生成绩
```
POST /api/v1/integration/push
Body: { "candidateId": "string", "candidateName": "string", "nationality": "string", "idNumber": "string", "email": "string", "totalScore": 0.0, "subjectScores": {}, "intention": "string?", "schoolId": "uuid", "round": 0 }
Response: { "code": 0, "data": { "pushId": "uuid" } }
```

### 考生确认录取事件
```
POST /api/v1/integration/candidate-confirmed
Body: { "candidateId": "string", "confirmedSchoolId": "uuid", "confirmedAt": "ISO8601" }
Response: { "code": 0 }
```

### 有条件录取到期事件
```
POST /api/v1/integration/condition-expired
Body: { "candidateId": "string", "schoolId": "uuid", "pushId": "uuid", "expiredAt": "ISO8601" }
Response: { "code": 0 }
```

---

## Mock API（开发环境）

Mock 服务模拟报名平台，供院校管理平台调用：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/mock-api/admission-notify` | POST | 录取通知 |
| `/mock-api/conditional-notify` | POST | 有条件录取通知 |
| `/mock-api/reject-notify` | POST | 拒绝通知 |
| `/mock-api/checkin-confirm` | POST | 报到确认 |
| `/mock-api/trigger/confirm` | GET | 触发考生确认事件 |
| `/mock-api/trigger/condition-expired` | GET | 触发条件到期事件 |

---

## 错误码

| 区间 | 模块 |
|------|------|
| 1xxxx | 账号/权限 |
| 2xxxx | 业务数据（院校、专业、名额等）|
| 3xxxx | 录取操作 |
| 4xxxx | 外部接口 |
| 5xxxx | 系统/服务端 |
