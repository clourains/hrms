# Fetch the HRMS open-api key and inject it into application.yml without printing it.
$ErrorActionPreference = 'Stop'

$loginBody = '{"username":"admin","password":"123456"}'
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post `
    -ContentType 'application/json; charset=utf-8' -Body $loginBody
$token = $login.data.token
if (-not $token) {
    Write-Host 'Login failed, no token returned.' -ForegroundColor Red
    exit 1
}

$cred = Invoke-RestMethod -Uri 'http://localhost:8080/api/open-api/credential' -Method Get `
    -Headers @{ Authorization = "Bearer $token" }
$apiKey = $cred.data.apiKey
if (-not $apiKey) {
    Write-Host 'No apiKey returned.' -ForegroundColor Red
    exit 1
}

# 写入当前用户环境变量，避免把 Key 落到 application.yml 进仓库
[Environment]::SetEnvironmentVariable('HRMS_API_KEY', $apiKey, 'User')
Write-Host 'HRMS api-key saved to user environment variable HRMS_API_KEY. Restart the ERP demo to take effect.'
