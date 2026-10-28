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

$yml = Join-Path $PSScriptRoot 'src\main\resources\application.yml'
$content = [System.IO.File]::ReadAllText($yml)
$content = [System.Text.RegularExpressions.Regex]::Replace($content, '(?m)(api-key:\s*).*$', "`${1}$apiKey")
[System.IO.File]::WriteAllText($yml, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'HRMS api-key injected into application.yml.'
