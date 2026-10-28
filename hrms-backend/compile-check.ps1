# 编译检查：使用当前 PATH 中的 JDK（项目要求 Java 17+），与 run-backend.ps1 保持一致
$javaExe = Get-Command java -ErrorAction SilentlyContinue
if ($javaExe) {
    $javaBinDir = Split-Path -Parent $javaExe.Source
    $javaHomeDir = Split-Path -Parent $javaBinDir
    $env:JAVA_HOME = $javaHomeDir
    Write-Host "Using JAVA_HOME: $javaHomeDir" -ForegroundColor DarkGray
} else {
    $env:JAVA_HOME = 'D:\jdk-17'
    Write-Host "java not found in PATH, fallback to $env:JAVA_HOME" -ForegroundColor Yellow
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

& "$PSScriptRoot\.mvn\apache-maven-3.9.16\bin\mvn.cmd" compile
exit $LASTEXITCODE
