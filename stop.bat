@echo off
chcp 65001 >nul
title HRMS 一键停止
color 0C

echo ================================================
echo   停止 HRMS 后端和前端
echo ================================================
echo.

echo [停止] 查找占用 8080 端口的进程 (后端) ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 " ^| findstr LISTENING') do (
    echo       终止 PID %%p
    taskkill /F /PID %%p >nul 2>&1
)

echo [停止] 查找占用 5173 / 5174 端口的进程 (前端) ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173 " ^| findstr LISTENING') do (
    echo       终止 PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5174 " ^| findstr LISTENING') do (
    echo       终止 PID %%p
    taskkill /F /PID %%p >nul 2>&1
)

echo.
echo 已停止所有服务。
pause
