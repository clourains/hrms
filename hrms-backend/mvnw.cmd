@REM Maven Wrapper - 自动下载Maven并运行
@REM 用法: mvnw.cmd <maven命令>
@REM 示例: mvnw.cmd spring-boot:run
@REM       mvnw.cmd clean package -DskipTests
@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "MAVEN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_HOME=%~dp0.mvn\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MAVEN_CMD%" (
    echo ====================================
    echo  首次运行: 正在下载 Maven %MAVEN_VERSION% ...
    echo ====================================
    if not exist "%~dp0.mvn" mkdir "%~dp0.mvn"
    powershell -Command "try { $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%~dp0.mvn\maven.zip' -UseBasicParsing; Expand-Archive -Path '%~dp0.mvn\maven.zip' -DestinationPath '%~dp0.mvn' -Force; Remove-Item '%~dp0.mvn\maven.zip'; Write-Host 'Maven下载成功!' } catch { Write-Host '下载失败:' $_.Exception.Message; exit 1 }"
    if errorlevel 1 (
        echo Maven下载失败，请检查网络连接
        exit /b 1
    )
    echo.
)

"%MAVEN_CMD%" %*
endlocal
