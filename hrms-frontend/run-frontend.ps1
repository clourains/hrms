# HRMS Frontend Startup Script
# Uses Node.js from TRAE VM environment

$NODE_DIR = "C:\Users\huzikang\AppData\Roaming\TRAE SOLO CN\ModularData\ai-agent\vm\tools\node"
$PROJECT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

# Add Node.js to PATH for this session
$env:PATH = "$NODE_DIR;" + $env:PATH

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  Node.js Path: $NODE_DIR" -ForegroundColor DarkGray
Write-Host "  Project Dir:  $PROJECT_DIR" -ForegroundColor DarkGray
Write-Host "====================================" -ForegroundColor Cyan

# Check if node_modules exists
$nodeModules = Join-Path $PROJECT_DIR "node_modules"
if (-not (Test-Path $nodeModules)) {
    Write-Host "  Installing dependencies (npm install)..." -ForegroundColor Yellow
    Write-Host "====================================" -ForegroundColor Cyan
    & "$NODE_DIR\npm.cmd" install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "npm install failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Dependencies installed successfully!" -ForegroundColor Green
}

Write-Host "  Starting Vue dev server..." -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

& "$NODE_DIR\npm.cmd" run dev
