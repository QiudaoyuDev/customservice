<#
.SYNOPSIS
  重启后端（Spring Boot）。先停止占用 8080 端口的旧进程，再启动新进程。

.PARAMETER Build
  启动前执行 mvn package 重新构建 jar。若 target 下没有 jar 也会自动构建。

.PARAMETER SkipTests
  构建时跳过测试（默认开启）。加 -SkipTests:$false 可跑测试。

.EXAMPLE
  .\start-backend.ps1            # 用已有 jar 直接重启
  .\start-backend.ps1 -Build     # 重新打包后再重启
#>
param(
    [switch]$Build,
    [switch]$SkipTests = $true
)

$ErrorActionPreference = 'Stop'

# ---- 路径与配置 ----
$ProjectRoot = (Get-Item $PSScriptRoot).Parent.FullName
$BackendDir  = Join-Path $ProjectRoot 'repos\backend'
$LogFile     = Join-Path $ProjectRoot 'backend-dev.log'
$ErrFile     = Join-Path $ProjectRoot 'backend-dev-err.log'
$Port        = 8080

$MavenWrapper = Join-Path $BackendDir 'mvnw.cmd'

# ---- 引入共用函数 ----
. "$PSScriptRoot\_common.ps1"

Write-Host "===== 后端启停（端口 $Port）====="

# 1) 停止旧的
Write-Host "[1/4] 停止旧的后端进程"
Stop-ProcessOnPort -Port $Port
Start-Sleep -Seconds 2
Stop-ProcessOnPort -Port $Port

# 2) 确保 PostgreSQL 已启动（后端依赖）
try {
    $svc = Get-Service postgresql-x64-16 -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -ne 'Running') {
        Write-Host "[2/4] 启动 PostgreSQL 服务"
        Start-Service postgresql-x64-16
    } elseif ($svc) {
        Write-Host "[2/4] PostgreSQL 服务已运行"
    }
} catch {
    Write-Warning "[2/4] 无法启动 PostgreSQL（可能缺少管理员权限）: $_"
}

# 3) 构建 jar（如需要）
Set-Location $BackendDir

$filter = { $_.Name -notmatch '(sources|javadoc)\.jar$' }
$jar = Get-ChildItem -Path target -Filter *.jar -ErrorAction SilentlyContinue |
       Where-Object $filter |
       Sort-Object LastWriteTime -Descending |
       Select-Object -First 1

if ($Build -or -not $jar) {
    Write-Host "[3/4] 构建 jar（Maven Wrapper package）"
    $mvnArgs = @('package')
    if ($SkipTests) { $mvnArgs += '-DskipTests' }
    & $MavenWrapper @mvnArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "mvn package 失败，退出"; exit 1 }

    $jar = Get-ChildItem -Path target -Filter *.jar |
           Where-Object $filter |
           Sort-Object LastWriteTime -Descending |
           Select-Object -First 1
}

if (-not $jar) { Write-Error "未找到可启动的 jar 包（target/*.jar）"; exit 1 }

# 4) 仅选择开发 profile；连接与密钥由开发者的环境变量提供，脚本不内置凭据。
Write-Host "[4/4] 启动后端：$($jar.Name)"
$env:SPRING_PROFILES_ACTIVE     = 'dev'

# 清空旧日志后启动，便于观察本次启动
if (Test-Path $LogFile) { Clear-Content $LogFile }
if (Test-Path $ErrFile) { Clear-Content $ErrFile }

$psi = @{
    FilePath               = 'java.exe'
    ArgumentList           = '-jar', $jar.FullName
    WorkingDirectory       = $BackendDir
    RedirectStandardOutput = $LogFile
    RedirectStandardError  = $ErrFile
    WindowStyle            = 'Hidden'
    PassThru               = $true
}
$proc = Start-Process @psi

Write-Host "      后端已启动 PID=$($proc.Id)"
Write-Host "      日志：$LogFile"

Wait-ForPort -Port $Port -Label '后端 '
