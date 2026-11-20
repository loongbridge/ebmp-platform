#!/usr/bin/env bash
# =====================================================================
#  EBMP 前端启动（Linux / macOS 原生）
#  用法：bash scripts/start-frontend.sh [PORT]
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

PORT="${1:-80}"
DIST="frontend/dist"
if [ ! -f "$DIST/index.html" ]; then
  echo "[ERROR] 未找到 $DIST/index.html" >&2; exit 1
fi

if command -v python3 >/dev/null 2>&1; then
  echo "[EBMP] python3 -m http.server $PORT (目录 $DIST)"
  nohup python3 -m http.server "$PORT" --directory "$DIST" > logs/frontend.log 2>&1 &
  echo $! > .frontend.pid
elif command -v node >/dev/null 2>&1; then
  echo "[EBMP] npx serve $DIST -l $PORT"
  mkdir -p logs
  nohup npx -y serve -l "$PORT" "$DIST" > logs/frontend.log 2>&1 &
  echo $! > .frontend.pid
else
  echo "[ERROR] 未检测到 python3 或 node" >&2; exit 1
fi
echo "[EBMP] 前端 PID=$(cat .frontend.pid)  访问：http://127.0.0.1:$PORT"
