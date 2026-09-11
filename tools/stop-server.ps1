# 一键终止：把 start-server.ps1 拉起来的东西全部关掉——MySQL、后端、前端、两个 Cloudflare 隧道。
# 用法：双击同目录下的 stop-server.bat（跟 start-server 一样，.ps1 本身双击不会真的执行）。
#   命令行等价写法：powershell -ExecutionPolicy Bypass -File tools\stop-server.ps1
#
# 关掉之后，队友手里的两个地址就都连不上了；再想用就重新跑一次 start-server.ps1（大概率会换新地址，
# 因为隧道进程已经不在了，重开的隧道地址跟之前不一样）。
# 这个脚本重复运行是安全的——本来就没在跑的部分会直接跳过，不会报错。

$root = Split-Path -Parent $PSScriptRoot

function Stop-PortProcess($port, $label) {
    $procIds = @()
    try {
        $procIds = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    } catch {}
    if (-not $procIds) {
        Write-Host "   $label`：没在跑，跳过。"
        return
    }
    foreach ($procId in $procIds) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "   $label`：已停止（端口 $port）。" -ForegroundColor Green
}

function Stop-Tunnel($name) {
    $pidFile = "$root\tools\.tunnel-$name.pid"
    if (Test-Path $pidFile) {
        $trackedPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($trackedPid) { Stop-Process -Id $trackedPid -Force -ErrorAction SilentlyContinue }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "1/4 前端隧道..." -ForegroundColor Cyan
Stop-Tunnel "frontend"

Write-Host "2/4 后端隧道..." -ForegroundColor Cyan
Stop-Tunnel "backend"

# 保险起见，把剩下的 cloudflared 进程也一并清掉——这台电脑上目前只有这个项目在用 cloudflared，
# 万一 .pid 文件对不上号（比如手动杀过一次进程），这一步能兜底，不会漏掉。
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "3/4 前端 / 后端..." -ForegroundColor Cyan
Stop-PortProcess 5173 "前端 Vite"
Stop-PortProcess 8080 "后端 Spring Boot"

Write-Host "4/4 本地 MySQL..." -ForegroundColor Cyan
Stop-PortProcess 3306 "MySQL"

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host " 全部停掉了。队友现在打不开那两个地址了，都是正常的。" -ForegroundColor Green
Write-Host " 下次要用，重新跑 start-server.ps1 就行——大概率会换一批新地址，跑完记得把新的前端地址再发一遍群里。" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Green
