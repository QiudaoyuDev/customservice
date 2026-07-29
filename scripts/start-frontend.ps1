<#
.SYNOPSIS
  重启前端（Vite）。先停止占用 5173 端口的旧进程，再启动新进程。

.PARAMETER Install
  启动前执行 npm ci 按锁文件安装依赖。若 node_modules 不存在也会自动安装。

.EXAMPLE
  .\start-frontend.ps1            # 用已有依赖直接重启
  .\start-frontend.ps1 -Install  # 先装依赖再重启
#>
param(
    [switch]$Install
)

$ErrorActionPreference = 'Stop'

# ---- 路径与配置 ----
$ProjectRoot = (Get-Item $PSScriptRoot).Parent.FullName
$FrontendDir = Join-Path $ProjectRoot 'repos\frontend'
$LogFile     = Join-Path $ProjectRoot 'frontend-dev.log'
$ErrFile     = Join-Path $ProjectRoot 'frontend-dev-err.log'
$Port        = 5173

# ---- 引入共用函数 ----
. "$PSScriptRoot\_common.ps1"

Write-Host "===== 前端启停（端口 $Port）====="

# 1) 停止旧的
Write-Host "[1/3] 停止旧的前端进程"
Stop-ProcessOnPort -Port $Port
Start-Sleep -Seconds 2
Stop-ProcessOnPort -Port $Port

# 2) 安装依赖（如需要）
Set-Location $FrontendDir

if ($Install -or -not (Test-Path node_modules)) {
    Write-Host "[2/3] 安装依赖（npm ci）"
    & npm.cmd ci
    if ($LASTEXITCODE -ne 0) { Write-Error "npm ci 失败，退出"; exit 1 }
} else {
    Write-Host "[2/3] 依赖已存在，跳过 npm ci"
}

# 3) 启动前端
Write-Host "[3/3] 启动前端（npm run dev）"

if (Test-Path $LogFile) { Clear-Content $LogFile }
if (Test-Path $ErrFile) { Clear-Content $ErrFile }

$psi = @{
    FilePath               = 'npm.cmd'
    ArgumentList           = 'run', 'dev'
    WorkingDirectory       = $FrontendDir
    RedirectStandardOutput = $LogFile
    RedirectStandardError  = $ErrFile
    WindowStyle            = 'Hidden'
    PassThru               = $true
}
$proc = Start-Process @psi

Write-Host "      前端已启动 PID=$($proc.Id)"
Write-Host "      日志：$LogFile"

Wait-ForPort -Port $Port -Label '前端 '
