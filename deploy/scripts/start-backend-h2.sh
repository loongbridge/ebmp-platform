#!/usr/bin/env bash
# =====================================================================
#  EBMP 后端启动脚本（H2 轻量版 · Linux / macOS）
#  前置：JDK 21+ 已安装且 java 在 PATH
#  用法：bash scripts/start-backend-h2.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

JAR="backend/ebmp-backend.jar"
if [ ! -f "$JAR" ]; then
  echo "[ERROR] 未找到 $JAR" >&2; exit 1
fi
if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] 未检测到 java，请先安装 JDK 21+" >&2; exit 1
fi

mkdir -p backend/data backend/uploads
mkdir -p logs

echo "[EBMP] 启动后端（H2 模式，后台运行，日志 logs/backend.log）"
nohup java -Xms256m -Xmx1024m -jar "$JAR" \
  --spring.profiles.active=h2 \
  > logs/backend.log 2>&1 &
echo $! > .backend.pid
echo "[EBMP] PID=$(cat .backend.pid)  健康检查：curl http://127.0.0.1:8080/api/health"
