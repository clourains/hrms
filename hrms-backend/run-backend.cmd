@REM 一键启动后端 - 无需安装Maven
@echo off
cd /d "%~dp0"
echo ====================================
echo  启动智能人事管理系统后端...
echo ====================================
call mvnw.cmd spring-boot:run
pause
