@echo off
REM =====================================================================
REM  EBMP 前端启动（Windows 原生，无需 Nginx）
REM  使用 Python 内置 http.server 或 Node serve 托管 dist 目录
REM  默认端口 80，可通过 SET PORT=8081 覆盖
REM =====================================================================
setlocal
chcp 65001 >nul 2>&1
cd /d "%~dp0.."

if not exist "frontend\dist\index.html" (
    echo [ERROR] 未找到 frontend\dist\index.html，请先部署前端产物。
    exit /b 1
)
if "%PORT%"=="" set PORT=80

where python >nul 2>&1
if errorlevel 1 goto :try_node

echo [EBMP] 使用 python -m http.server 托管前端 dist（端口 %PORT%）
echo [EBMP] 说明：纯静态托管不会反向代理 /api，前端将直连后端 http://127.0.0.1:8080
pushd frontend\dist
start "EBMP-Frontend" cmd /k "python -m http.server %PORT%"
popd
goto :eof

:try_node
where npx >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 python 或 node，请先安装其中之一来托管前端。
    exit /b 1
)
echo [EBMP] 使用 npx serve 托管前端 dist（端口 %PORT%）
pushd frontend\dist
start "EBMP-Frontend" cmd /k "npx -y serve -l %PORT% ."
popd
endlocal
