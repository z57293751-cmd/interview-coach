# 一键部署到云服务器（在本地 Windows 上运行）
# 用法：开一个 PowerShell 窗口，cd 到本目录，执行  .\deploy.ps1
#       （不要用"右键运行"，那样出错窗口会一闪而过）

param(
    [string]$Server = "",
    [string]$User   = "ubuntu"
)

# 注意：这里故意用 Continue，不用 Stop。
# 因为 scp/ssh 会往 stderr 写东西，PS 5.1 在 Stop 模式下会直接中断脚本。
$ErrorActionPreference = 'Continue'

$deployDir  = $PSScriptRoot
$projectDir = Split-Path -Parent $deployDir
$javaHome   = 'C:\Program Files\Java\jdk-17'
$maven      = 'C:\apache-maven-3.5.4\bin\mvn.cmd'
$jarName    = 'interview-coach-0.1.0.jar'
$remoteDir  = '/home/ubuntu/coach-deploy'

function Pause-Exit([string]$msg, [int]$code = 1) {
    Write-Host ''
    if ($msg) { Write-Host $msg -ForegroundColor Red }
    Read-Host '按回车退出'
    exit $code
}

Write-Host ''
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host '      部署 AI 面试小站到云服务器' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host ''

if (-not $Server) {
    $Server = Read-Host '  服务器公网 IP（例如 129.204.36.182）'
}
if (-not $Server) { Pause-Exit '  没填 IP' }

$target = "$User@$Server"

# ---------- 1. 打包 ----------
$jar = Join-Path $projectDir "target\$jarName"
if (-not (Test-Path $jar)) {
    Write-Host '[1/4] 没找到 jar，开始打包…' -ForegroundColor Yellow
    $env:JAVA_HOME = $javaHome
    Push-Location $projectDir
    & $maven -B -DskipTests package 2>&1 | ForEach-Object { Write-Host "   $_" -ForegroundColor DarkGray }
    Pop-Location
    if (-not (Test-Path $jar)) { Pause-Exit '  打包失败，看上面的报错' }
} else {
    Write-Host '[1/4] jar 已存在，跳过打包' -ForegroundColor Green
}

# ---------- 2. 组装上传包 ----------
Write-Host '[2/4] 组装上传包…' -ForegroundColor Yellow
$stage = Join-Path $env:TEMP ("coach-deploy-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage -Force | Out-Null

Copy-Item $jar (Join-Path $stage $jarName) -Force
foreach ($f in @('schema.sql', 'seed.sql', 'mock_schema.sql')) {
    $src = Join-Path $deployDir $f
    if (Test-Path $src) { Copy-Item $src (Join-Path $stage $f) -Force }
    else { Write-Host "  [警告] 缺少 $f" -ForegroundColor Yellow }
}

# setup.sh 必须是 LF 换行 + 无 BOM，否则 bash 报 $'\r': command not found
$shText = (Get-Content (Join-Path $deployDir 'setup.sh') -Raw) -replace "`r`n", "`n"
[System.IO.File]::WriteAllText((Join-Path $stage 'setup.sh'), $shText, (New-Object System.Text.UTF8Encoding($false)))

$tgz = Join-Path $env:TEMP 'coach-deploy.tar.gz'
if (Test-Path $tgz) { Remove-Item $tgz -Force }
& tar.exe -czf $tgz -C $stage . 2>&1 | Out-Null
if (-not (Test-Path $tgz)) { Pause-Exit '  打包压缩失败' }
Write-Host ("  打包完成：{0} MB" -f [math]::Round((Get-Item $tgz).Length / 1MB, 1)) -ForegroundColor Green

# ---------- 3. 上传 ----------
Write-Host ''
Write-Host "[3/4] 上传到 $Server …" -ForegroundColor Yellow
Write-Host '      现在要输 root 密码（输入时不显示，粘完直接回车）' -ForegroundColor DarkGray
Write-Host ''

& scp.exe -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 $tgz "${target}:/home/ubuntu/" 2>&1 |
    ForEach-Object { Write-Host "   $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host "  scp 退出码: $LASTEXITCODE" -ForegroundColor Red
    Pause-Exit @'
  上传失败。逐个排查：
   1. 密码错 —— 去腾讯云控制台「重置密码」，重启后重试
   2. 腾讯云默认禁止 root 密码登录 —— 控制台里开启「密码登录」
   3. 22 端口不通 —— 防火墙要放行 22
   4. IP 写错
'@
}

# ---------- 4. 远程执行 ----------
Write-Host ''
Write-Host '[4/4] 在服务器上安装（会再要一次密码）…' -ForegroundColor Yellow
Write-Host ''

& ssh.exe -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 $target @"
mkdir -p $remoteDir && tar xzf /home/ubuntu/coach-deploy.tar.gz -C $remoteDir && cd $remoteDir && sed -i 's/\r$//' setup.sh && sudo bash setup.sh
"@ 2>&1 | ForEach-Object { Write-Host "   $_" }

$sshCode = $LASTEXITCODE

Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $tgz -Force -ErrorAction SilentlyContinue

Write-Host ''
if ($sshCode -eq 0) {
    Write-Host '========================================' -ForegroundColor Green
    Write-Host '  完成！上面打印的网址就是你的新地址' -ForegroundColor Green
    Write-Host '========================================' -ForegroundColor Green
} else {
    Write-Host "  ssh 退出码: $sshCode，看上面的输出" -ForegroundColor Red
}

Read-Host '按回车退出（窗口会关）'
