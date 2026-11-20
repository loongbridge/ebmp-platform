@echo off
REM =====================================================================
REM  EBMP 后端启动脚本（MySQL 生产版 · Windows）
REM  前置：JDK 21+、MySQL 8.x 已创建 ebmp 库
REM  首次使用请编辑同目录 application-mysql.yml，或设置下面三个变量
REM =====================================================================
setlocal
chcp 65001 >nul 2>&1
cd /d "%~dp0.."

set DB_URL=%MYSQL_URL%
if "%DB_URL%"=="" set DB_URL=jdbc:mysql://127.0.0.1:3306/ebmp?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&useSSL=false
set DB_USER=%MYSQL_USER%
if "%DB_USER%"=="" set DB_USER=root
set DB_PASS=%MYSQL_PASSWORD%
if "%DB_PASS%"=="" set DB_PASS=root

if not exist "backend\ebmp-backend.jar" (
    echo [ERROR] 未找到 backend\ebmp-backend.jar，请先部署产物。
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 java，请先安装 JDK 21+ 并配置 PATH。
    exit /b 1
)

if not exist "backend\uploads" mkdir "backend\uploads"

echo [EBMP] 启动后端（MySQL 模式）...
echo        URL      = %DB_URL%
echo        USER     = %DB_USER%
echo        PASSWORD = ****
echo        如需修改数据库地址/账号，可在当前窗口设置 MYSQL_URL、MYSQL_USER、MYSQL_PASSWORD 后再执行本脚本。

start "EBMP-Backend-MySQL" cmd /k "java -Xms512m -Xmx2048m -jar backend\ebmp-backend.jar ^
  --spring.profiles.active=mysql ^
  --spring.datasource.url=%DB_URL% ^
  --spring.datasource.username=%DB_USER% ^
  --spring.datasource.password=%DB_PASS%"

echo [EBMP] 等待后端就绪（最长 90 秒）...
for /l %%i in (1,1,90) do (
    timeout /t 1 /nobreak >nul
    powershell -NoProfile -Command "try { Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 1 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
    if not errorlevel 1 (
        echo [EBMP] 后端已就绪：http://127.0.0.1:8080
        goto :eof
    )
)
echo [WARN] 健康检查超时，请确认 MySQL 是否启动、账号密码是否正确。
endlocal
