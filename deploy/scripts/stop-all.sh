#!/usr/bin/env bash
# =====================================================================
#  EBMP 停止脚本
# =====================================================================
cd "$(dirname "$0")/.."
if [ -f .backend.pid ]; then
  kill "$(cat .backend.pid)" 2>/dev/null && echo "[EBMP] 后端已停止 PID=$(cat .backend.pid)" || echo "[EBMP] 后端进程已不存在"
  rm -f .backend.pid
fi
if [ -f .frontend.pid ]; then
  kill "$(cat .frontend.pid)" 2>/dev/null && echo "[EBMP] 前端已停止 PID=$(cat .frontend.pid)" || echo "[EBMP] 前端进程已不存在"
  rm -f .frontend.pid
fi
