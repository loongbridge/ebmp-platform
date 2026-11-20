@echo off
REM =====================================================================
REM  EBMP 后端启动脚本（H2 轻量版 · Windows）
REM  前置：JDK 21+ 已安装且 java 在 PATH 中
REM  访问：http://127.0.0.1:8080/api/health
REM =====================================================================
setlocal
chcp 65001 >nul 2>&1
cd /d "%~dp0.."

if not exist "backend\ebmp-backend.jar" (
    echo [ERROR] 未找到 backend\ebmp-backend.jar，请先部署产物。
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 java，请先安装 JDK 21+ 并配置 PATH。
    exit /b 1
)

echo [EBMP] 启动后端（H2 文件库模式，profile=h2）...
echo [EBMP] 数据目录：%cd%\backend\data
echo [EBMP] 上传目录：%cd%\backend\uploads

if not exist "backend\data" mkdir "backend\data"
if not exist "backend\uploads" mkdir "backend\uploads"

start "EBMP-Backend" cmd /k "java -Xms256m -Xmx1024m -jar backend\ebmp-backend.jar --spring.profiles.active=h2"

echo [EBMP] 等待后端就绪（最长 60 秒）...
for /l %%i in (1,1,60) do (
    timeout /t 1 /nobreak >nul
    powershell -NoProfile -Command "try { Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 1 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
    if not errorlevel 1 (
        echo [EBMP] 后端已就绪：http://127.0.0.1:8080
        goto :eof
    )
)
echo [WARN] 健康检查超时，请查看新开的后端窗口日志。
endlocal
