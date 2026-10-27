@echo off
setlocal EnableDelayedExpansion
title HRMS Startup

set "ROOT=%~dp0"
set "BACKEND=%ROOT%hrms-backend"
set "FRONTEND=%ROOT%hrms-frontend"
set "TOOLS=%LOCALAPPDATA%\TraeTools"
set "JDK17=%TOOLS%\jdk-17"
set "REDIS_DIR=%TOOLS%\redis"

echo ================================================
echo   HRMS - One-Click Start
echo ================================================
echo.

echo [1/7] Checking project folders ...
if not exist "%BACKEND%\pom.xml" goto NO_BACKEND
if not exist "%FRONTEND%\package.json" goto NO_FRONTEND
echo       Folders OK
echo.

echo [2/7] Checking Java 17+ ...
set "JAVA_HOME="
if exist "%JDK17%\bin\java.exe" (
    set "JAVA_HOME=%JDK17%"
    echo       Bundled JDK 17 found
) else (
    where java >nul 2>&1
    if errorlevel 1 goto NO_JAVA
    set "SYSHOME="
    for /f "delims=" %%p in ('where java') do if not defined SYSHOME set "SYSHOME=%%p"
    set "SYSHOME=!SYSHOME:\bin\java.exe=!"
    set "JAVA_HOME=!SYSHOME!"
    set "JVER="
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined JVER set "JVER=%%v"
    set "JVER=!JVER:"=!"
    set "JMAJOR="
    for /f "delims=. tokens=1" %%m in ("!JVER!") do set "JMAJOR=%%m"
    if not defined JMAJOR goto NO_JAVA
    if !JMAJOR! LSS 17 goto OLD_JAVA
    echo       System Java !JVER! OK
)
echo       JAVA_HOME=!JAVA_HOME!
echo.

echo [3/7] Checking Node.js ...
where node >nul 2>&1
if errorlevel 1 goto NO_NODE
where npm >nul 2>&1
if errorlevel 1 goto NO_NODE
echo       Node OK
echo.

echo [4/7] Checking backend Maven ...
if not exist "%BACKEND%\.mvn\apache-maven-3.9.16\bin\mvn.cmd" (
    echo       [WARN] Bundled Maven not found, will try run-backend.ps1 instead
)
echo       OK
echo.

echo [5/7] Checking MySQL (port 3306) ...
call :PORT_OPEN 3306
if not errorlevel 1 (
    echo       MySQL OK
) else (
    echo       MySQL not running - trying to start service MySQL ...
    net start MySQL >nul 2>&1
    call :WAIT_PORT 3306 4
    if errorlevel 1 (
        echo       Need admin rights - a UAC dialog will pop up, please click Yes
        powershell -NoProfile -Command "Start-Process -FilePath cmd.exe -ArgumentList '/c net start MySQL' -Verb RunAs" >nul 2>&1
        call :WAIT_PORT 3306 15
    )
    if errorlevel 1 (
        echo       [ERROR] MySQL is NOT running on 3306.
        echo       Please start MySQL manually: right-click D:\mysql-8.0.46 + run the .vbs there as admin
    ) else (
        echo       MySQL started OK
    )
)
echo.

echo       Ensuring EMPTY database 'hrms' exists (Flyway builds the schema) ...
set "MYSQL_EXE="
for %%p in ("%MYSQL_HOME%\bin\mysql.exe" "D:\mysql-8.0.46\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe") do (
    if not defined MYSQL_EXE (
        if exist "%%~p" set "MYSQL_EXE=%%~p"
    )
)
if not defined MYSQL_EXE (
    for /f "delims=" %%p in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%p"
)
if defined MYSQL_EXE (
    "!MYSQL_EXE!" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS hrms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" >nul 2>&1
    if errorlevel 1 (
        echo       [WARN] Could not reach MySQL with root/123456 on 3306.
        echo       [WARN] Create an EMPTY database named 'hrms' manually before the backend starts.
    ) else (
        echo       Database 'hrms' ready (schema comes from Flyway migrations)
    )
) else (
    echo       [WARN] mysql client not found - skipped auto-create.
    echo       [WARN] Make sure an EMPTY database 'hrms' exists, or the backend will fail on startup.
)
echo.

echo [6/7] Checking Redis (port 6379) ...
call :PORT_OPEN 6379
if not errorlevel 1 (
    echo       Redis OK
) else (
    if exist "%REDIS_DIR%\redis-server.exe" (
        set "REDIS_CONF="
        if exist "%REDIS_DIR%\hrms-redis.conf" set "REDIS_CONF=hrms-redis.conf"
        echo       Starting local Redis on 6379 ...
        if defined REDIS_CONF (
            start "HRMS-Redis (6379)" /D "%REDIS_DIR%" cmd /k "redis-server.exe %REDIS_CONF%"
        ) else (
            start "HRMS-Redis (6379)" /D "%REDIS_DIR%" cmd /k "redis-server.exe --port 6379 --requirepass 123456"
        )
        call :WAIT_PORT 6379 8
        echo       Redis started
    ) else (
        echo       [WARN] Redis not found at %REDIS_DIR%
        echo       [WARN] Token blacklist / department cache will fail without Redis
    )
)
echo.

echo [7/7] Starting backend and frontend in 2 windows
echo       Close a window to stop that service
echo.

echo Starting backend on 8080 ...
start "HRMS-Backend (8080)" /D "%BACKEND%" cmd /k ".mvn\apache-maven-3.9.16\bin\mvn.cmd -f pom.xml spring-boot:run"

echo Wait 8s for backend ...
call :SLEEP 8

echo Starting frontend on 5173 ...
start "HRMS-Frontend (5173)" /D "%FRONTEND%" cmd /k "npm run dev"

echo.
echo ================================================
echo   Open in browser: http://localhost:5173
echo   Default account: admin / 123456
echo   Backend API   : http://localhost:8080/api
echo ================================================
echo.
echo Press any key to open the browser
pause >nul
start "" "http://localhost:5173"
exit /b 0

:PORT_OPEN
netstat -an | findstr ":%~1 " | findstr "LISTENING" >nul 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:SLEEP
ping -n %~1 127.0.0.1 >nul 2>&1
exit /b 0

:WAIT_PORT
setlocal
set "WP_PORT=%~1"
set "WP_TRIES=%~2"
for /l %%i in (1,1,%WP_TRIES%) do (
    netstat -an | findstr ":%WP_PORT% " | findstr "LISTENING" >nul 2>&1
    if not errorlevel 1 ( endlocal & exit /b 0 )
    call :SLEEP 3
)
endlocal & exit /b 1

:NO_BACKEND
echo [ERROR] Backend folder not found: %BACKEND%
goto FAIL
:NO_FRONTEND
echo [ERROR] Frontend folder not found: %FRONTEND%
goto FAIL
:NO_JAVA
echo [ERROR] Java not found. Need JDK 17+.
goto FAIL
:OLD_JAVA
echo [ERROR] Java version is too old for this release. Need JDK 17+.
echo         Bundled JDK expected at: %JDK17%
goto FAIL
:NO_NODE
echo [ERROR] Node.js / npm not found. Install Node.js 16+ first.
goto FAIL
:FAIL
echo.
echo Press any key to exit
pause >nul
exit /b 1