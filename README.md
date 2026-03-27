# 院校管理平台

> 中国政府奖学金项目留学生招生录取管理系统

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| 后端 | Spring Boot 3.2 / Java 21 / Maven |
| 前端 | Vue 3 / Vite 5 / TypeScript / Element Plus |
| 数据库 | PostgreSQL 15 |
| 缓存 | Redis 7 |
| 文件存储 | MinIO |
| 容器化 | Docker + docker-compose |

## 快速启动（开发环境）

### 前置条件
- JDK 21+
- Maven 3.9+
- Node.js 20+
- Docker Desktop

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d postgres redis minio
```

### 2. 初始化数据库

DDL 迁移脚本由 Flyway 自动执行，首次启动后端时会自动创建全部 16 张表。

### 3. 启动后端

```bash
cd backend
# 首次需要先生成 JWT 密钥（目录）
mkdir -p src/main/resources/keys
# 开发环境使用对称密钥，无需手动生成
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动后访问 Swagger UI：`http://localhost:8080/swagger-ui.html`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端：`http://localhost:5173`

### 5. 启动 Mock 服务（可选）

```bash
cd mock-server
mvn spring-boot:run
```

Mock API：`http://localhost:8081`

## 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 运营管理员 | op_admin | OpAdmin@2026 | 全局管理员 |

> ⚠️ 首次登录后强制要求修改密码。

## 项目结构

```
campus-platform/
├── backend/           Spring Boot 后端（Java 21）
├── frontend/          Vue 3 前端
├── mock-server/       报名平台 Mock 服务
├── docker/           Docker 配置
└── docs/             补充文档
```

## 环境变量

复制 `.env.example` 为 `.env` 并填入实际值。

## 数据库

- 共 16 张表，详见 `backend/src/main/resources/db/migration/V1__init_schema.sql`
- 使用 Flyway 进行迁移管理
- 审计日志表（audit_log）按月分区

## 部署

### 生产环境 Docker 部署

```bash
cp .env.example .env
# 编辑 .env 填入生产配置
docker-compose -f docker/docker-compose.prod.yml up -d
```

### 更新后端

```bash
cd backend && mvn package -DskipTests
docker-compose -f docker/docker-compose.prod.yml up -d --build backend
```

## 接口文档

启动后端后访问：`http://localhost:8080/swagger-ui.html`

## 开发规范

- 分支命名：`feature/`、`fix/`、`chore/`
- 提交信息：遵循 Conventional Commits
- 代码风格：IDEA/.editorconfig
