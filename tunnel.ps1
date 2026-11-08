# 把本机 8080 暴露到公网（Cloudflare Tunnel，免注册）
# 用法：先运行 run.ps1 把应用起来，再运行本脚本
# 说明：窗口里会打印一个 https://xxx.trycloudflare.com 地址；
#       关掉窗口隧道就断；每次重开地址会变，但不会像 serveo 那样十分钟就过期。

$ErrorActionPreference = 'Stop'

$cloudflared = 'D:\deepseek\works\tools\cloudflared.exe'

# 先确认应用活着
try {
    $null = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/ping' -TimeoutSec 5 -UseBasicParsing
} catch {
    Write-Host '[错误] 8080 没在跑，先执行 run.ps1' -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}

if (-not (Test-Path $cloudflared)) {
    Write-Host "[错误] 找不到 cloudflared：$cloudflared" -ForegroundColor Red
    Write-Host '可以从 https://github.com/cloudflare/cloudflared/releases 下载 cloudflared-windows-amd64.exe' -ForegroundColor Yellow
    Read-Host '按回车退出'
    exit 1
}

Write-Host ''
Write-Host '  正在建立公网隧道，稍等几秒…' -ForegroundColor Green
Write-Host '  看到 https://xxxx.trycloudflare.com 那一行就是你的公网地址' -ForegroundColor Cyan
Write-Host '  这个窗口别关，关了隧道就断' -ForegroundColor DarkGray
Write-Host ''

# --protocol http2 比默认的 QUIC 更稳（有些网络会重置 QUIC）
& $cloudflared tunnel --url http://localhost:8080 --no-autoupdate --protocol http2
