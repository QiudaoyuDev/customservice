<#
.SYNOPSIS
  前后端启停脚本共用的辅助函数。
  被 start-backend.ps1 / start-frontend.ps1 通过 dot-source 引入。
#>

# 停止占用指定 TCP 端口的所有进程（"停止旧的"）
function Stop-ProcessOnPort {
    param([int]$Port)

    try {
        $conns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        $ids   = $conns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -ne 0 }

        if (-not $ids) {
            Write-Host "  [info] 端口 $Port 当前没有被占用"
            return
        }

        foreach ($id in $ids) {
            try {
                $name = (Get-Process -Id $id -ErrorAction SilentlyContinue).Name
                Write-Host "  [stop] 终止占用端口 $Port 的进程 PID=$id ($name)"
                Stop-Process -Id $id -Force -ErrorAction Stop
            } catch {
                Write-Warning "  [stop] 无法终止 PID=$id : $_"
            }
        }
    } catch {
        Write-Warning "  [stop] 查询端口 $Port 失败（建议以管理员身份运行）: $_"
    }
}

# 轮询等待端口可连接（"确认新的已启动"）
function Wait-ForPort {
    param(
        [int]   $Port,
        [string]$Label = '',
        [int]   $TimeoutSec = 120
    )

    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        $ok = Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
        if ($ok) {
            Write-Host "  [ok] ${Label}端口 $Port 已就绪"
            return $true
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    Write-Warning "  [warn] 等待 ${Label}端口 $Port 超时（${TimeoutSec}s），请查看日志排查"
    return $false
}
