#!/usr/bin/env bash
# =====================================================================
#  EBMP 后端启动脚本（MySQL 生产版 · Linux / macOS）
#  用法：MYSQL_URL=jdbc:mysql://... MYSQL_USER=root MYSQL_PASSWORD=root \
#          bash scripts/start-backend-mysql.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

: "${MYSQL_URL:=jdbc:mysql://127.0.0.1:3306/ebmp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}"
: "${MYSQL_USER:=root}"
: "${MYSQL_PASSWORD:=root}"

JAR="backend/ebmp-backend.jar"
if [ ! -f "$JAR" ]; then echo "[ERROR] 未找到 $JAR" >&2; exit 1; fi

mkdir -p backend/uploads logs

echo "[EBMP] 启动后端（MySQL 模式）：$MYSQL_USER@$MYSQL_URL"
nohup java -Xms512m -Xmx2048m -jar "$JAR" \
  --spring.profiles.active=mysql \
  --spring.datasource.url="$MYSQL_URL" \
  --spring.datasource.username="$MYSQL_USER" \
  --spring.datasource.password="$MYSQL_PASSWORD" \
  > logs/backend.log 2>&1 &
echo $! > .backend.pid
echo "[EBMP] PID=$(cat .backend.pid)  日志：tail -f logs/backend.log"
