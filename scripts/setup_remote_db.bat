@echo off
REM ============================================================================
REM SCRIPT DE INICIALIZACION DE BASE DE DATOS REMOTA (MySQL)
REM Autores: Gaizka, Xiker, Diego
REM ============================================================================

echo ----------------------------------------------------------------------------
echo [1/2] Creando Base de Datos (Schema)...
echo ----------------------------------------------------------------------------
python scripts/create_remote_db.py
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al crear la base de datos.
    echo Verifique su conexion y el archivo .env
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ----------------------------------------------------------------------------
echo [2/2] Creando Tablas (Users, Favorites, Characters)...
echo ----------------------------------------------------------------------------
python scripts/init_remote_tables.py
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo al inicializar las tablas.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================================================
echo [EXITO] BASE DE DATOS Y TABLAS CREADAS CORRECTAMENTE
echo ============================================================================
pause
