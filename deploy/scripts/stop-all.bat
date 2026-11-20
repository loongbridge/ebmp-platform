@echo off
REM =====================================================================
REM  EBMP 停止脚本（停止后端 & 前端进程）
REM =====================================================================
chcp 65001 >nul 2>&1
echo [EBMP] 尝试停止 EBMP-Backend / EBMP-Frontend 窗口...
powershell -NoProfile -Command ^
"Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*ebmp-backend.jar*' -or $_.CommandLine -like '*ebmp-backend-1.0.0.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host ('  已停止 PID ' + $_.ProcessId) }"
echo [EBMP] 已完成。
