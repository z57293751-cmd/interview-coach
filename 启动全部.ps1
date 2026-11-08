# 一键启动：检查 MySQL -> 起应用 -> 开公网隧道
# 用法：右键本文件 ->「使用 PowerShell 运行」
# 可选：.\启动全部.ps1 -NoTunnel    （只起应用，不开公网）

param([switch]$NoTunnel)

$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }

function Test-App {
    try {
        $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/ping' -TimeoutSec 3 -UseBasicParsing
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

Write-Host ''
Write-Host '======================================' -ForegroundColor DarkGray
Write-Host '        AI 面试小站 一键启动' -ForegroundColor Cyan
Write-Host '======================================' -ForegroundColor DarkGray

# ---------- 1. MySQL ----------
$mysql = Get-Service MySQL80 -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Host '[1/3] 没找到 MySQL80 服务' -ForegroundColor Red
} elseif ($mysql.Status -ne 'Running') {
    Write-Host '[1/3] MySQL 没启动，正在启动…' -ForegroundColor Yellow
    Start-Service MySQL80 -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    Write-Host ('      现在状态：' + (Get-Service MySQL80).Status)
} else {
    Write-Host '[1/3] MySQL 运行中 OK' -ForegroundColor Green
}

# ---------- 2. 应用 ----------
if (Test-App) {
    Write-Host '[2/3] 应用已经在 8080 上运行，跳过启动' -ForegroundColor Green
} else {
    Write-Host '[2/3] 正在启动应用（会在新窗口里跑）…' -ForegroundColor Yellow
    Start-Process powershell -ArgumentList @('-NoExit', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $root 'run.ps1')) | Out-Null

    $ok = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        if (Test-App) { $ok = $true; break }
        Write-Host '.' -NoNewline
    }
    Write-Host ''
    if ($ok) {
        Write-Host '      应用起来了 OK' -ForegroundColor Green
    } else {
        Write-Host '      等了 80 秒应用还没起来，去看那个新窗口里的红色报错' -ForegroundColor Red
        Read-Host '按回车退出'
        exit 1
    }
}

if ($NoTunnel) {
    Write-Host '[3/3] 按参数要求跳过公网隧道' -ForegroundColor DarkGray
    Write-Host ''
    Write-Host '本机地址：http://localhost:8080/' -ForegroundColor Cyan
    Read-Host '按回车退出'
    exit 0
}

# ---------- 3. 公网隧道 ----------
Write-Host '[3/3] 正在开公网隧道，稍等几秒…' -ForegroundColor Yellow
Write-Host ''
Write-Host '  下面出现 https://xxxx.trycloudflare.com 就是你今天的公网地址' -ForegroundColor Cyan
Write-Host '  这个窗口别关；按 Ctrl+C 停止' -ForegroundColor DarkGray
Write-Host ''

$cf = 'D:\deepseek\works\tools\cloudflared.exe'
if (-not (Test-Path $cf)) {
    Write-Host "[错误] 找不到 cloudflared：$cf" -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}
& $cf tunnel --url http://localhost:8080 --no-autoupdate --protocol http2
