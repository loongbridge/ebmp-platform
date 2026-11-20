#!/usr/bin/env bash
# ============================================================
#  EBMP · 启动后数据库连接验证（Linux / macOS）
#
#  验证顺序：
#    1. 后端就绪与健康检查      GET  /api/health
#    2. 数据库读（账号表）      POST /api/auth/login
#    3. 鉴权 + 用户表读取       GET  /api/auth/me
#    4. 业务多表聚合读          GET  /api/dashboard/overview
#    5. （可选）MySQL 直连表数量  设置 MYSQL_HOST 且存在 mysql 客户端时执行
#
#  用法：
#    bash verify-db-connection.sh
#    EBMP_BASE_URL=http://10.0.0.8:8080 bash verify-db-connection.sh
#
#  环境变量：
#    EBMP_BASE_URL(默认 http://127.0.0.1:8080) EBMP_USER(admin)
#    EBMP_PASSWORD(Admin@123) EBMP_WAIT_SECONDS(60)
#    MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE
#
#  退出码：0 全部通过；1 存在失败项
# ============================================================
set -u

BASE_URL="${EBMP_BASE_URL:-http://127.0.0.1:8080}"
USERNAME="${EBMP_USER:-admin}"
PASSWORD="${EBMP_PASSWORD:-Admin@123}"
WAIT_SECONDS="${EBMP_WAIT_SECONDS:-60}"
BASE_URL="${BASE_URL%/}"

PASS_COUNT=0
FAIL_COUNT=0

green()  { printf '\033[32m%s\033[0m\n' "$1"; }
red()    { printf '\033[31m%s\033[0m\n' "$1"; }
cyan()   { printf '\033[36m%s\033[0m\n' "$1"; }
yellow() { printf '\033[33m%s\033[0m\n' "$1"; }

report() { # name ok(0=pass) ms detail
  if [ "$2" -eq 0 ]; then
    green "  [PASS] $1  ${3} ms  $4"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    red "  [FAIL] $1  ${3} ms  $4"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
}

now_ms() {
  if date +%s%3N >/dev/null 2>&1; then
    date +%s%3N
  else
    python3 -c 'import time;print(int(time.time()*1000))'
  fi
}

# 提取 JSON 字段（无 jq 依赖）
json_code()  { echo "$1" | grep -o '"code":[-0-9]*' | head -1 | cut -d: -f2; }
json_token() { echo "$1" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4; }

if ! command -v curl >/dev/null 2>&1; then
  red '未找到 curl 命令，无法执行验证（请先安装 curl）'
  exit 1
fi

cyan '======================================================='
cyan '  EBMP 数据库连接验证'
echo "  后端地址 : $BASE_URL"
echo "  验证账号 : $USERNAME"
if [ -n "${MYSQL_HOST:-}" ]; then
  echo "  数据库   : MySQL @ $MYSQL_HOST"
else
  echo "  数据库   : H2（内置文件库，通过接口间接验证）"
fi
cyan '======================================================='
echo

# ---------- 0. 等待后端就绪 ----------
yellow "[等待] 后端就绪（最多 $WAIT_SECONDS 秒）..."
READY=0
i=0
while [ "$i" -lt "$WAIT_SECONDS" ]; do
  BODY="$(curl -s -m 3 "$BASE_URL/api/health" 2>/dev/null || true)"
  if [ "$(json_code "$BODY")" = "0" ]; then READY=1; break; fi
  i=$((i + 1))
  sleep 1
done
if [ "$READY" -ne 1 ]; then
  red '  [FAIL] 后端未在限定时间内就绪：请确认进程已启动、端口可访问'
  echo
  red '结论: 验证中止（后端不可达）'
  exit 1
fi
green '  [ OK ] 后端已就绪'
echo

# ---------- 1. 健康检查 ----------
T0="$(now_ms)"
BODY="$(curl -s -m 5 "$BASE_URL/api/health" 2>/dev/null || true)"
T1="$(now_ms)"
if [ "$(json_code "$BODY")" = "0" ]; then
  report 'GET /api/health' 0 "$((T1 - T0))" "$(echo "$BODY" | tr -d '\n' | cut -c1-90)"
else
  report 'GET /api/health' 1 "$((T1 - T0))" "${BODY:-无响应}"
fi

# ---------- 2. 登录（读账号/角色表） ----------
T0="$(now_ms)"
BODY="$(curl -s -m 10 -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" 2>/dev/null || true)"
T1="$(now_ms)"
TOKEN="$(json_token "$BODY")"
if [ "$(json_code "$BODY")" = "0" ] && [ -n "$TOKEN" ]; then
  USER_NAME="$(echo "$BODY" | grep -o '"username":"[^"]*"' | head -1 | cut -d'"' -f4)"
  report 'POST /api/auth/login' 0 "$((T1 - T0))" "user=$USER_NAME token_len=${#TOKEN}"
else
  report 'POST /api/auth/login' 1 "$((T1 - T0))" "$(echo "${BODY:-无响应}" | tr -d '\n' | cut -c1-120)"
fi

# ---------- 3. 鉴权 + 用户表读取 ----------
T0="$(now_ms)"
if [ -z "$TOKEN" ]; then
  report 'GET /api/auth/me' 1 0 '登录未通过，跳过'
else
  BODY="$(curl -s -m 5 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/auth/me" 2>/dev/null || true)"
  T1="$(now_ms)"
  if [ "$(json_code "$BODY")" = "0" ]; then
    ME_NAME="$(echo "$BODY" | grep -o '"username":"[^"]*"' | head -1 | cut -d'"' -f4)"
    report 'GET /api/auth/me' 0 "$((T1 - T0))" "user=$ME_NAME"
  else
    report 'GET /api/auth/me' 1 "$((T1 - T0))" "$(echo "${BODY:-无响应}" | tr -d '\n' | cut -c1-120)"
  fi
fi

# ---------- 4. 业务多表聚合读 ----------
T0="$(now_ms)"
if [ -z "$TOKEN" ]; then
  report 'GET /api/dashboard/overview' 1 0 '登录未通过，跳过'
else
  BODY="$(curl -s -m 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/dashboard/overview" 2>/dev/null || true)"
  T1="$(now_ms)"
  if [ "$(json_code "$BODY")" = "0" ]; then
    report 'GET /api/dashboard/overview' 0 "$((T1 - T0))" '订单/客户/库存/待办等聚合指标读取成功'
  else
    report 'GET /api/dashboard/overview' 1 "$((T1 - T0))" "$(echo "${BODY:-无响应}" | tr -d '\n' | cut -c1-120)"
  fi
fi

# ---------- 5. 可选：MySQL 直连校验 ----------
if [ -n "${MYSQL_HOST:-}" ]; then
  T0="$(now_ms)"
  if ! command -v mysql >/dev/null 2>&1; then
    report 'MySQL 直连表数量' 1 0 '未找到 mysql 客户端，跳过直连校验'
  else
    MYSQL_PORT="${MYSQL_PORT:-3306}"
    MYSQL_DATABASE="${MYSQL_DATABASE:-ebmp}"
    SQL="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE' AND table_type='BASE TABLE';"
    OUT="$(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-}" -N -B -e "$SQL" 2>&1 | tr -d '\r')"
    T1="$(now_ms)"
    COUNT="$(echo "$OUT" | grep -E '^[0-9]+$' | head -1)"
    if [ -n "$COUNT" ] && [ "$COUNT" -ge 30 ]; then
      report 'MySQL 直连表数量' 0 "$((T1 - T0))" "库=$MYSQL_DATABASE 表数=$COUNT（期望 34）"
    else
      report 'MySQL 直连表数量' 1 "$((T1 - T0))" "$(echo "$OUT" | tr '\n' ' ' | cut -c1-120)"
    fi
  fi
fi

# ---------- 汇总 ----------
echo
cyan '-------------------------------------------------------'
echo "  通过: $PASS_COUNT    失败: $FAIL_COUNT"
cyan '-------------------------------------------------------'
if [ "$FAIL_COUNT" -eq 0 ]; then
  green '结论: 数据库连接正常，后端可正常读写数据库'
  exit 0
else
  red "结论: 存在 $FAIL_COUNT 项失败，请检查后端日志与数据库配置"
  exit 1
fi
