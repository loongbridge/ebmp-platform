@echo off
rem ============================================================
rem  EBMP database connection verification (Windows wrapper)
rem  Calls verify-db-connection.ps1 with the same arguments.
rem  Exit code: 0 = all checks passed, 1 = failed
rem ============================================================
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0verify-db-connection.ps1" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
