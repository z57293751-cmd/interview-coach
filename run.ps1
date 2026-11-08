# AI 面试小站 - 一键启动脚本
# 用法：右键「使用 PowerShell 运行」，或在本目录执行  .\run.ps1
# 前置：本机 MySQL 8 已启动，库 coach 已建好

$ErrorActionPreference = 'Stop'

# 脚本所在目录（用 $PSScriptRoot，比 $MyInvocation 可靠）
$here = $PSScriptRoot
if (-not $here) { $here = (Get-Location).Path }

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'

# ---------- 数据库 ----------
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_PASSWORD = '123456'

# ---------- 访问口令 ----------
# 设了之后，别人打开地址要先输这串口令才能进；留空则完全不设防（只适合本机自用）
if (-not $env:ACCESS_PASSWORD) { $env:ACCESS_PASSWORD = 'coach2026' }

# ---------- DeepSeek ----------
# key 优先用已有环境变量；没有就从本机凭据文件里取，避免明文写进仓库
if (-not $env:DEEPSEEK_API_KEY) {
    $cred = Join-Path $env:USERPROFILE '.dsh\.credentials.yaml'
    if (Test-Path $cred) {
        $m = [regex]::Match((Get-Content $cred -Raw), 'DEEPSEEK_API_KEY:\s*(\S+)')
        if ($m.Success) { $env:DEEPSEEK_API_KEY = $m.Groups[1].Value }
    }
}
if (-not $env:DEEPSEEK_API_KEY) {
    Write-Host '[错误] 没找到 DEEPSEEK_API_KEY，请先设置环境变量' -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}

# flash 评分约 3 秒；v4-pro 约 20-35 秒，不建议用在前端同步请求里
$env:DEEPSEEK_MODEL = 'deepseek-flash'
$env:DEEPSEEK_TIMEOUT_SECONDS = '60'

# ---------- 定位 jar ----------
$jar = Join-Path $here 'target\interview-coach-0.1.0.jar'
if (-not (Test-Path $jar)) {
    Write-Host '[错误] 没找到 jar：' -ForegroundColor Red
    Write-Host "  $jar" -ForegroundColor Yellow
    Write-Host '先执行打包：' -ForegroundColor Yellow
    Write-Host '  $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"' -ForegroundColor Yellow
    Write-Host '  C:\apache-maven-3.5.4\bin\mvn.cmd -DskipTests package' -ForegroundColor Yellow
    Read-Host '按回车退出'
    exit 1
}

$javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $javaExe)) {
    Write-Host "[错误] 找不到 java.exe：$javaExe" -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}

Write-Host ''
Write-Host '  AI 面试小站 启动中...' -ForegroundColor Green
Write-Host '  题库：http://localhost:8080/question.html' -ForegroundColor Cyan
Write-Host '  按 Ctrl+C 停止' -ForegroundColor DarkGray
Write-Host ''

& $javaExe -jar $jar
