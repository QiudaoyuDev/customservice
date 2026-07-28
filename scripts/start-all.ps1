<#
.SYNOPSIS
  一键重启前后端。先分别停止旧的（8080 / 5173），再依次启动后端与前端。

.PARAMETER Build
  后端启动前重新 mvn package。
.PARAMETER SkipTests
  后端构建时跳过测试（默认开启）。
.PARAMETER Install
  前端启动前执行 npm install。

.EXAMPLE
  .\start-all.ps1                    # 用已有 jar / 依赖直接重启
  .\start-all.ps1 -Build -Install    # 重新构建后端并安装前端依赖后重启
#>
param(
    [switch]$Build,
    [switch]$SkipTests = $true,
    [switch]$Install
)

$ErrorActionPreference = 'Stop'

# 引入共用函数（先停两端旧进程）
. "$PSScriptRoot\_common.ps1"

Write-Host "############################################"
Write-Host "#        重启前后端（先停后启）            #"
Write-Host "############################################"

# 1) 先停掉两端旧进程
Write-Host ">> 停止旧的前后端进程"
Stop-ProcessOnPort -Port 8080
Stop-ProcessOnPort -Port 5173
Start-Sleep -Seconds 2

# 2) 依次启动（子脚本内部也会再次确认端口无占用）
& "$PSScriptRoot\start-backend.ps1" -Build:$Build -SkipTests:$SkipTests
& "$PSScriptRoot\start-frontend.ps1" -Install:$Install

Write-Host ""
Write-Host "===== 前后端已全部启动 ====="
Write-Host "前端: http://localhost:5173"
Write-Host "后端: http://localhost:8080"
