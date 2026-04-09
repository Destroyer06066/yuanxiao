# 补录管理流程图

## 1. 整体录取流程

```mermaid
flowchart TB
    subgraph external["外部系统"]
        RP[报名平台]
    end

    subgraph round_mgmt["轮次管理 OP_ADMIN"]
        RM1[创建第1轮<br/>MODE_1 考生推送模式]
        RM2[创建第2轮及以后<br/>MODE_2 邀请模式]
        RA[开启轮次 ACTIVE]
        RC[关闭轮次 CLOSED]
        RD[删除轮次]
    end

    subgraph mode1["模式一 MODE_1（第1轮）"]
        direction TB
        M1P[考生数据推送<br/>来自报名平台]
        M1S[院校处理考生<br/>录取/拒绝/有条件录取]
        M1C[考生确认录取]
        M1M[材料收取]
        M1CK[考生报到 CHECKED_IN]
    end

    subgraph mode2["模式二 MODE_2（第2轮及以后）"]
        direction TB
        M2S[院校检索考生]
        M2I[发送录取邀请]
        M2R[考生响应邀请<br/>接受/拒绝/过期]
    end

    RP -->|推送考生数据| M1P
    RM1 --> RA
    RM2 --> RA
    RA -->|在有效期| mode1
    RA -->|MODE_2启用| mode2
    RC -->|轮次结束| mode1
    RC -->|轮次结束| mode2

    M1P --> M1S
    M1S -->|录取 ADMITTED| M1C
    M1S -->|拒绝 REJECTED| M1S
    M1S -->|有条件录取 CONDITIONAL| M1S
    M1C -->|CONFIRMED| M1M
    M1M --> M1CK

    M2S --> M2I
    M2I --> M2R
    M2R -->|拒绝/过期| M2S
```

## 2. 轮次生命周期

```mermaid
stateDiagram-v2
    [*] --> UPCOMING: 创建轮次
    UPCOMING --> ACTIVE: 开启轮次
    ACTIVE --> CLOSED: 关闭轮次
    CLOSED --> ACTIVE: 重新开启
    UPCOMING --> [*]: 删除
    CLOSED --> [*]: 删除

    note right of UPCOMING
        轮次已创建
        未开始
    end note

    note right of ACTIVE
        正在进行中
        考生可被处理
        邀请可被发送
    end note

    note right of CLOSED
        轮次已结束
        不能再处理考生
    end note
```

## 3. 模式一（第1轮）考生状态流转

```mermaid
flowchart TB
    P["PENDING (待处理)"]

    subgraph operation["操作分支"]
        AD["ADMITTED (直接录取)"]
        CD["CONDITIONAL (有条件录取)"]
        RJ["REJECTED (拒绝)"]
    end

    subgraph conditionalBranch["有条件录取分支"]
        CD --> CDT["满足条件"]
        CD --> CDF["INVALIDATED (条件到期失效)"]
        CDT --> AD
    end

    P --> AD
    P --> CD
    P --> RJ

    AD -->|"考生确认"| CF["CONFIRMED (已确认)"]
    CF --> MR["MATERIAL_RECEIVED (材料已收)"]
    MR --> CK["CHECKED_IN (已报到)"]
    CK --> END1("END")

    RJ -->|"可重新推送"| P
    AD -->|"考生拒绝"| RJ
    AD -->|"录取超时"| INV["INVALIDATED (录取失效)"]
    INV -->|"可重新推送"| P

    MR -->|"放弃"| RJ
    CF -->|"考生放弃"| RJ

    P -->|"被他校录取"| EE["ENROLLED_ELSEWHERE (已被他校录取)"]
    EE --> END2("END")
```

## 4. 模式二（第2轮）邀请流程

```mermaid
flowchart TB
    subgraph schoolOp["院校操作"]
        QS["检索考生条件"]
        SS["选择考生"]
        SM["选择录取专业"]
        SMsg["填写邀请留言"]
        SSI["发送邀请"]
    end

    subgraph candidateResp["考生响应"]
        IAC["INVITED (邀请已发出)"]
        IAR["ACCEPTED (考生接受)"]
        IARJ["REJECTED (考生拒绝)"]
        IAEX["EXPIRED (邀请过期)"]
    end

    QS --> SS
    SS --> SM
    SM --> SMsg
    SMsg --> SSI

    SSI -->|"预占名额"| IAC
    IAC --> IAR
    IAC --> IARJ
    IAC --> IAEX

    IAR -->|"转为正式录取"| AD["ADMITTED (已录取)"]
    IARJ -->|"考生可被再次邀请"| QS
    IAEX -->|"名额释放"| QS

    AD --> CF["CONFIRMED (已确认)"]
    CF --> MR["MATERIAL_RECEIVED (材料已收)"]
    MR --> CK["CHECKED_IN (已报到)"]
    CK --> END3("END")
```

## 5. 补录轮次管理流程

```mermaid
flowchart TB
    subgraph OP_ADMIN操作
        START[OP_ADMIN登录]
        CR[创建录取轮次]
        CN[轮次号自动设置]
        CM[模式自动设置<br/>第1轮=MODE_1<br/>第2轮+=MODE_2]
        ST[设置开始/结束时间]
        SR[设置备注]
    end

    START --> CR
    CR --> CN
    CN --> CM
    CM --> ST
    ST --> SR
    SR --> DONE[轮次创建完成<br/>状态=UPCOMING]

    subgraph 轮次状态管理
        ACT[开启轮次] --> |校验无其他进行中| ACTIVE
        ACTIVE --> |倒计时结束| WARN[周期进行中提示]
        ACTIVE --> CL[关闭轮次]
        CL --> CLOSED
    end

    DONE --> ACT
    CLOSED --> ACT
```

## 6. 考生数据推送流程（外部集成）

```mermaid
sequenceDiagram
    participant RP as 报名平台
    participant API as 录取平台API
    participant DB as 数据库

    RP->>API: POST /push<br/>推送考生成绩数据
    API->>API: 校验数据完整性
    API->>API: 判断推送轮次<br/>第1轮=MODE_1
    API->>DB: 写入candidate_push<br/>status=PENDING
    API->>RP: 返回成功

    Note over RP,DB: 考生状态初始为 PENDING

    alt 条件录取到期
        RP->>API: POST /condition-expired
        API->>DB: 更新status=INVALIDATED
    end

    alt 考生确认录取
        RP->>API: POST /candidate-confirmed
        API->>DB: 更新status=CONFIRMED
    end
```

## 7. 补录邀请流程（模式二）

```mermaid
sequenceDiagram
    participant School as 院校用户
    participant FE as 前端
    participant API as 录取平台API
    participant RP as 报名平台
    participant DB as 数据库

    School->>FE: 检索符合条件的考生
    FE->>API: GET /v1/students<br/>status=PENDING/REJECTED/INVALIDATED
    API->>DB: 查询考生数据
    DB-->>FE: 返回考生列表
    FE-->>School: 显示检索结果

    School->>FE: 选择考生 & 发送邀请
    FE->>API: POST /v1/supplement/invitations
    API->>DB: 创建邀请记录<br/>status=INVITED
    API->>DB: 预占名额reservedCount+1
    API->>RP: 通知报名平台
    RP-->>School: 显示邀请状态

    alt 考生接受
        RP->>API: 回调确认
        API->>DB: status=ACCEPTED<br/>考生正式录取
    end

    alt 考生拒绝/过期
        RP->>API: 回调通知
        API->>DB: status=REJECTED/EXPIRED
        API->>DB: 释放名额reservedCount-1
    end
```

## 8. 完整考生状态枚举

```mermaid
flowchart TB
    subgraph statuses["考生状态"]
        P["PENDING (待处理)"]
        C["CONDITIONAL (有条件录取)"]
        A["ADMITTED (已录取待确认)"]
        CF["CONFIRMED (已确认)"]
        MR["MATERIAL_RECEIVED (材料已收)"]
        CK["CHECKED_IN (已报到)"]
        RJ["REJECTED (已拒绝)"]
        INV["INVALIDATED (录取失效)"]
        EE["ENROLLED_ELSEWHERE (被他校录取)"]
        IV["INVITED (已发出邀请)"]
    end

    P --> C
    P --> A
    P --> RJ
    C --> A
    C --> INV
    A --> CF
    A --> RJ
    A --> INV
    CF --> MR
    CF --> RJ
    MR --> CK
    MR --> RJ
    CK --> END4("END")

    P --> EE
    RJ --> P
    INV --> P

    A -.-> IV
```

## 关键说明

| 概念 | 说明 |
|------|------|
| **MODE_1（第1轮）** | 考生推送模式，报名平台主动推送考生数据，院校处理录取 |
| **MODE_2（第2轮+）** | 邀请模式，院校主动检索并邀请符合条件考生 |
| **UPCOMING** | 轮次已创建但未开启 |
| **ACTIVE** | 轮次进行中 |
| **CLOSED** | 轮次已结束 |
| **INVITED** | 模式二下，院校已向考生发送录取邀请 |
| **canBeInvited()** | 只有 PENDING/REJECTED/INVALIDATED 状态的考生可被邀请 |
