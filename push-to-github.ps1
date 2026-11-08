# 把项目推到 GitHub（交互式，Token 只在内存里用，不落盘）
# 用法：右键本文件 ->「使用 PowerShell 运行」

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
$git = 'D:\deepseek\works\tools\MinGit\cmd\git.exe'

if (-not (Test-Path $git)) {
    Write-Host "[错误] 找不到 git：$git" -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}

Write-Host ''
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host '        推送到 GitHub' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor DarkGray
Write-Host ''

# 1. 仓库地址
Write-Host '第 1 步：仓库地址' -ForegroundColor Yellow
Write-Host '  在 GitHub 建好空仓库后，把地址粘过来，例如：'
Write-Host '  https://github.com/lujintao/interview-coach.git'
$remote = Read-Host '  地址'
if (-not $remote) { Write-Host '没填，退出'; exit 1 }
$remote = $remote.Trim()
if ($remote -notmatch '\.git$') { $remote = $remote.TrimEnd('/') + '.git' }
if ($remote -notmatch '^https://') {
    Write-Host '  地址要以 https:// 开头' -ForegroundColor Red
    Read-Host '按回车退出'
    exit 1
}

# 2. Token（输入时隐藏）
Write-Host ''
Write-Host '第 2 步：Personal Access Token' -ForegroundColor Yellow
Write-Host '  GitHub -> Settings -> Developer settings -> Personal access tokens'
Write-Host '  -> Fine-grained tokens -> 勾 Contents: Read and write'
$secure = Read-Host '  粘贴 Token（输入时不显示）' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$token = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

if (-not $token) { Write-Host '没填 Token，退出'; exit 1 }

Set-Location $root
Write-Host ''
Write-Host '正在推送…' -ForegroundColor Green

try {
    # 先设一个干净的 remote（不带 token）
    & $git remote remove origin 2>$null | Out-Null
    & $git remote add origin $remote 2>&1 | Out-Null

    # 推送时临时把 token 放进 URL，推完立即把 remote 改回干净地址
    $tokenUrl = $remote -replace '^https://', "https://$token@"
    & $git push $tokenUrl "HEAD:main" 2>&1 | ForEach-Object { Write-Host '  ' + $_ }

    if ($LASTEXITCODE -eq 0) {
        & $git branch -M main 2>&1 | Out-Null
        & $git branch --set-upstream-to=origin/main main 2>&1 | Out-Null
        Write-Host ''
        Write-Host '推送成功！' -ForegroundColor Green
        Write-Host "仓库地址：$remote" -ForegroundColor Cyan
        Write-Host ''
        Write-Host '以后改完代码，推上去只要三条命令：' -ForegroundColor DarkGray
        Write-Host '  git add -A'
        Write-Host '  git commit -m "改了什么"'
        Write-Host '  git push'
    } else {
        Write-Host ''
        Write-Host '推送失败，常见原因：' -ForegroundColor Red
        Write-Host '  1. Token 没勾 Contents: Read and write'
        Write-Host '  2. Token 过期了'
        Write-Host '  3. 仓库名写错，或者仓库里已经有文件（建仓库时别勾 README）'
    }
} catch {
    Write-Host ('出错：' + $_.Exception.Message) -ForegroundColor Red
} finally {
    # 无论如何都把 token 从内存里抹掉，并确认 remote 里没有 token
    $token = $null
    $tokenUrl = $null
    & $git remote set-url origin $remote 2>&1 | Out-Null
    Write-Host ''
    Write-Host '（Token 已清除，remote 地址里不含 Token）' -ForegroundColor DarkGray
    Read-Host '按回车退出'
}
