# HRMS Backend Startup Script
# Auto-downloads Maven if not installed
$ErrorActionPreference = "Stop"

$PROJECT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$MAVEN_VERSION = "3.9.16"
$MAVEN_DIR = Join-Path $PROJECT_DIR ".mvn\apache-maven-$MAVEN_VERSION"
$MAVEN_CMD = Join-Path $MAVEN_DIR "bin\mvn.cmd"

# Use Java 21 if available, otherwise fall back to JAVA_HOME
$javaExe = Get-Command java -ErrorAction SilentlyContinue
if ($javaExe) {
    $javaBinDir = Split-Path -Parent $javaExe.Source
    $javaHomeDir = Split-Path -Parent $javaBinDir
    $env:JAVA_HOME = $javaHomeDir
    Write-Host "Using JAVA_HOME: $javaHomeDir" -ForegroundColor DarkGray
}

if (-not (Test-Path $MAVEN_CMD)) {
    Write-Host "====================================" -ForegroundColor Cyan
    Write-Host "  Downloading Maven $MAVEN_VERSION ..." -ForegroundColor Cyan
    Write-Host "====================================" -ForegroundColor Cyan

    $mvnDir = Join-Path $PROJECT_DIR ".mvn"
    if (-not (Test-Path $mvnDir)) {
        New-Item -ItemType Directory -Force -Path $mvnDir | Out-Null
    }

    $zipPath = Join-Path $mvnDir "maven.zip"
    $mirrors = @(
        "https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip",
        "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip",
        "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"
    )

    $downloaded = $false
    foreach ($url in $mirrors) {
        try {
            Write-Host "  Trying: $url" -ForegroundColor Gray
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing -TimeoutSec 60
            Expand-Archive -Path $zipPath -DestinationPath $mvnDir -Force
            Remove-Item $zipPath
            Write-Host "Maven downloaded successfully!" -ForegroundColor Green
            $downloaded = $true
            break
        }
        catch {
            Write-Host "  Failed: $($_.Exception.Message)" -ForegroundColor DarkGray
        }
    }

    if (-not $downloaded) {
        Write-Host "All mirrors failed. Please download Maven manually." -ForegroundColor Red
        Write-Host "Download from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        Write-Host "Extract to: $mvnDir" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  Starting HRMS Backend..." -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

$arguments = $args
if ($arguments.Count -eq 0) {
    $arguments = @("spring-boot:run")
}

& $MAVEN_CMD @arguments
