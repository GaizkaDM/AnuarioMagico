@echo off
cd /d "%~dp0"
echo ===================================================
echo   AUTOMATIC DATA SYNC
echo ===================================================

echo.
echo [1/3] Starting Backend Server (in new window)...
start "Harry Potter Backend" cmd /k ".venv\Scripts\python.exe backend\app.py"

echo.
echo [2/3] Waiting 8 seconds for server to start...
timeout /t 8 /nobreak >nul

echo.
echo [3/3] Running Resync Script...
.venv\Scripts\python.exe resync.py

echo.
echo ===================================================
echo   SYNC COMPLETE
echo ===================================================
echo.
pause
