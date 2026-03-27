#!/bin/bash
# 院校管理平台 — 本地启动脚本
# 使用方法: ./start.sh

set -e

echo "=== 院校管理平台启动脚本 ==="

# 1. 启动基础设施
echo "[1/3] 启动 PostgreSQL / Redis / MinIO / Mock 服务..."
cd "$(dirname "$0")/docker"
docker-compose up -d postgres redis minio mock-server

echo "  等待服务就绪..."
sleep 8

# 2. 初始化数据库（Flyway 自动执行，此处仅提示）
echo "[2/3] 数据库迁移将由后端自动执行（Flyway）"

# 3. 提示启动方式
echo "[3/3] 基础设施已就绪！"
echo ""
echo "下一步："
echo "  后端: cd ../backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "  前端: cd ../frontend && npm install && npm run dev"
echo ""
echo "访问地址："
echo "  前端:  http://localhost:5173"
echo "  后端:  http://localhost:8080"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo "  MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "默认账号: op_admin / OpAdmin@2026"
