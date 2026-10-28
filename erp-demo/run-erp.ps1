param([switch]$Background)

# Start the ERP demo (Spring Boot, port 8090)
$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = 'D:\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$projectRoot = Split-Path -Parent $PSScriptRoot
$localMvn = Join-Path $projectRoot 'hrms-backend\.mvn\apache-maven-3.9.16\bin\mvn.cmd'
if (Test-Path $localMvn) {
    $mvn = $localMvn
} else {
    $mvn = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
}

if (-not $mvn -or -not (Test-Path $mvn)) {
    Write-Host "Maven not found. Install Maven or keep this folder inside the HRMS repo." -ForegroundColor Red
    exit 1
}

if ($Background) {
    $logFile = Join-Path $PSScriptRoot 'erp-demo.log'
    $errFile = Join-Path $PSScriptRoot 'erp-demo.err.log'
    $proc = Start-Process -FilePath 'powershell.exe' `
        -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"") `
        -WorkingDirectory $PSScriptRoot -WindowStyle Hidden `
        -RedirectStandardOutput $logFile -RedirectStandardError $errFile -PassThru
    Write-Host "ERP demo started in background. PID=$($proc.Id)"
    Write-Host "Log: $logFile"
    exit 0
}

Push-Location $PSScriptRoot
try {
    & $mvn spring-boot:run
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $exitCode
