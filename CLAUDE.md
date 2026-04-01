# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

院校管理平台（campus-platform）是一个多租户招生录取管理系统，支持中国政府奖学金项目留学生招生。

## 用户偏好
- **所有开发，修复工作完成后，都要用浏览器测试，通过后才可以反馈工作完成。用有头模式测试，第一优先使用 Chrome DevTools MCP，第二使用 Agent-Browser

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端 | Spring Boot 3.4 / Java 21 / Maven |
| 前端 | Vue 3 / Vite 5 / TypeScript / Element Plus / Pinia |
| 数据库 | PostgreSQL 15 |
| 缓存 | Redis 7 |
| 文件存储 | MinIO |
| 数据库迁移 | Flyway |
| API文档 | SpringDoc OpenAPI (Swagger UI) |

## 快速启动

### 开发环境启动

```bash
# 1. 启动基础设施（Docker）
cd docker && docker-compose up -d postgres redis minio

# 2. 启动后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 或使用JAR: java -jar target/campus-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# 3. 启动前端
cd frontend && npm install && npm run dev
```

### 端口

- 前端: http://localhost:5173 (开发) / 5174 (备用)
- 后端: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001

### 默认账号

- OP_ADMIN: op_admin / OpAdmin@2026

## 常用命令

### 后端

```bash
# 编译打包
cd backend && mvn compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn package -DskipTests

# 清理并重新打包
mvn clean package -DskipTests
```

### 前端

```bash
cd frontend

# 开发模式
npm run dev

# 类型检查
npm run type-check

# ESLint检查
npm run lint

# 运行测试
npm run test

# 生产构建
npm run build
```

## 架构

### 后端分层

```
com.campus.platform/
├── controller/     # REST API控制器
├── service/       # 业务逻辑
├── repository/    # 数据访问（MyBatis-Plus）
├── entity/        # 实体类
│   └── enums/     # 枚举（PENDING, ADMITTED, CONFIRMED等考生状态）
├── dto/           # 数据传输对象
├── integration/    # 外部系统集成
│   ├── inbound/   # 接收外部回调（报名平台推送）
│   └── outbound/  # 调用外部API
├── security/      # Spring Security + JWT认证
├── schedule/      # 定时任务
└── common/        # 通用组件（Result, Exception等）
```

### 核心业务实体

- **Account**: 用户账号（OP_ADMIN, SCHOOL_ADMIN, SCHOOL_STAFF）
- **School**: 院校
- **Major**: 专业
- **AdmissionQuota**: 名额配置
- **CandidatePush**: 考生推送记录（核心业务表）
- **SupplementRound**: 补录轮次

### 前端分层

```
frontend/src/
├── views/           # 页面组件
│   ├── admin/       # OP_ADMIN专属页面
│   ├── school/      # 院校通用页面
│   └── common/      # 公共页面（Login, Layout, Dashboard）
├── stores/          # Pinia状态管理（auth, notification）
├── api/             # API调用封装
├── router/          # Vue Router路由配置
└── composables/     # Vue Composition API复用
```

### 角色权限

| 角色 | 说明 |
|------|------|
| OP_ADMIN | 运营管理员 - 全局管理 |
| SCHOOL_ADMIN | 院校管理员 |
| SCHOOL_STAFF | 院校工作人员 |

前端路由通过 `meta.roles` 控制权限，后端通过 `@RequireRole` 注解。

### 数据库迁移

迁移文件位于 `backend/src/main/resources/db/migration/`：
- V1__init_schema.sql: 初始16张表
- V2-V7: 增量迁移

### 外部集成

**报名平台集成** (`/api/v1/integration/*`)：
- `/push`: 接收考生成绩推送
- `/candidate-confirmed`: 考生确认录取回调
- `/condition-expired`: 条件录取到期回调
- `/invitation-callback`: 补录邀请回调

这些接口无鉴权（IP白名单在Nginx层配置）。

## 业务概念

### 考生状态流转

```
PENDING → CONDITIONAL → ADMITTED → CONFIRMED → MATERIAL_RECEIVED → CHECKED_IN
    ↓           ↓            ↓
 REJECTED    INVALIDATED   REJECTED/INVALIDATED
    ↓                    ↓
 (可重新推送)         (可重新推送)
```

### 补录周期（模式一）

通过 `supplement_round` 表管理轮次：
- UPCOMING: 即将开始
- ACTIVE: 进行中（考生可重新推送）
- CLOSED: 已结束

### 补录邀请（模式二）

院校主动检索并邀请符合条件考生，状态新增 `INVITED`。

### 系统参数

存储在 `school_config` 表，`school_id=NULL` 表示全局配置。

## 注意事项

1. **本地PostgreSQL冲突**: 本地可能已有PostgreSQL服务，Docker容器端口冲突时使用localhost:5432连接本地而非容器。
2. **Java版本**: 项目要求Java 21，但编译产物可在Java 17+运行。
3. **数据库重建**: 需要重建数据库时，先删除再创建：先`pg_terminate_backend`断开连接，再`DROP DATABASE`。
4. **前端端口**: Vite开发服务器可能占用5173/5174，视启动情况而定。
