﻿# HRMS One-Click Startup Script (PowerShell)
# Usage: double-click start.bat (it calls this file)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  HRMS - One-Click Start" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root "hrms-backend"
$frontend = Join-Path $root "hrms-frontend"

if (-not (Test-Path (Join-Path $backend "pom.xml"))) {
    Write-Host "[ERROR] Backend not found: $backend" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
if (-not (Test-Path (Join-Path $frontend "package.json"))) {
    Write-Host "[ERROR] Frontend not found: $frontend" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[1/3] Checking Java ..."
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "[ERROR] Java not found. Install JDK 17+ or 21 first." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "      Java OK" -ForegroundColor Green

Write-Host "[2/3] Checking Node.js ..."
$node = Get-Command node -ErrorAction SilentlyContinue
if (-not $node) {
    Write-Host "[ERROR] Node.js not found. Install Node.js 16+ first." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "      Node OK" -ForegroundColor Green

Write-Host "[3/3] Checking MySQL (port 3306) ..." -ForegroundColor DarkGray
try {
    $mysqlOk = (Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue).TcpTestSucceeded
} catch { $mysqlOk = $false }
if (-not $mysqlOk) {
    Write-Host "[WARN ] MySQL 3306 not reachable. Start MySQL first!" -ForegroundColor Yellow
} else {
    Write-Host "      MySQL 3306 OK" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Starting backend + frontend (2 new windows)" -ForegroundColor Cyan
Write-Host "  Close a window to stop that service" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Build cmd command strings (using cmd.exe /k to set title + cd)
# This avoids PowerShell 5.1 missing -WindowTitle parameter entirely.
$backendCmd = 'title HRMS-Backend (8080) & cd /d "' + $backend + '" & powershell -NoExit -NoProfile -ExecutionPolicy Bypass -File run-backend.ps1'
$frontendCmd = 'title HRMS-Frontend (5173) & cd /d "' + $frontend + '" & npm run dev'

Write-Host "Starting backend ..." -ForegroundColor DarkGray
Start-Process cmd.exe -ArgumentList "/k", $backendCmd

Write-Host "Wait 5s for backend ..."
Start-Sleep -Seconds 5

Write-Host "Starting frontend ..." -ForegroundColor DarkGray
Start-Process cmd.exe -ArgumentList "/k", $frontendCmd

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Frontend OK -> open: http://localhost:5173"
Write-Host "  Default: admin / 123456"
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to open browser"
Start-Process "http://localhost:5173"
