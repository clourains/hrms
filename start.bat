@echo off
chcp 65001 >nul
title HRMS Startup
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
