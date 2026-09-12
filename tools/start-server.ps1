# 在"主机"这台电脑上运行：一键拉起 MySQL + 后端 + 前端，并用 Cloudflare Tunnel 各开一个公网地址。
# 队友什么都不用装、不用 clone 代码——你把跑完打印出来的【前端地址】发到群里，他们浏览器打开就能用。
# 数据都落在这台电脑的 MySQL 里，谁注册谁的都在一起。
#
# 用法：直接双击同目录下的 start-server.bat（这个 .ps1 本身双击不会真的执行，Windows 默认不这么处理 .ps1）。
#   命令行等价写法：powershell -ExecutionPolicy Bypass -File tools\start-server.ps1
#
# 这个脚本重复运行是安全的——已经在跑的部分会跳过，隧道地址只要进程还活着就沿用旧的、不会变。
#
# 说明：
#  - 免费不记名隧道地址（xxx.trycloudflare.com）在真正重新开隧道时会换一个新的（比如这台电脑重启过）。
#    换了就得把新地址重新发一遍群里。想要永久固定地址，得注册 Cloudflare 账号配命名隧道，或者把整套
#    部署到云服务器，这个脚本先不做。
#  - 这台电脑不能关机、不能休眠，MySQL / 后端 / 前端 / 两个隧道这几个进程也不能关，不然队友就连不上了。

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.4\bin"
$mysqlData = "D:\tools\mysql-data"
$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

function Test-Port($port) {
    Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
}

function Get-TunnelUrl($logFile) {
    if (-not (Test-Path $logFile)) { return $null }
    $m = Select-String -Path $logFile -Pattern "https://[a-z0-9-]+\.trycloudflare\.com" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($m) { return $m.Matches[0].Value }
    return $null
}

# 隧道按名字管理：进程还活着就复用旧地址，死了才重开。PID 记在 .tunnel-<name>.pid 里。
function Ensure-Tunnel($port, $name) {
    $log = "$root\tools\tunnel-$name.log"
    $pidFile = "$root\tools\.tunnel-$name.pid"
    if ((Test-Path $pidFile)) {
        $oldPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
            $u = Get-TunnelUrl $log
            if ($u) { return $u }
        }
    }
    if (Test-Path $log) { Remove-Item $log -Force }
    $p = Start-Process -FilePath $cloudflared -ArgumentList "tunnel", "--url", "http://localhost:$port" `
        -RedirectStandardError $log -WindowStyle Minimized -PassThru
    $p.Id | Set-Content $pidFile
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        $u = Get-TunnelUrl $log
        if ($u) { return $u }
    }
    return $null
}

function Stop-PortProcess($port) {
    try {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique |
            ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
    } catch {}
}

# ---------- 1) MySQL ----------
Write-Host "1/5 本地 MySQL..." -ForegroundColor Cyan
if (Test-Port 3306) {
    Write-Host "   已经在跑了，跳过。"
} else {
    Start-Process -FilePath "$mysqlBin\mysqld.exe" -ArgumentList "--datadir=$mysqlData", "--port=3306" -WindowStyle Minimized
    Start-Sleep -Seconds 4
}

# ---------- 2) 后端 ----------
Write-Host "2/5 后端（Spring Boot）..." -ForegroundColor Cyan
$backendLog = "$root\tools\backend.log"
if (Test-Port 8080) {
    Write-Host "   已经在跑了，跳过。"
} else {
    if (Test-Path $backendLog) { Remove-Item $backendLog -Force }
    # 坑 1：mvn 在 Windows 上是 mvn.cmd，Start-Process -FilePath "mvn" 经常悄悄启动失败，得包 cmd.exe /c。
    # 坑 2：Spring Boot 在临时目录建的哈希工作目录会做属主校验，从不同方式（PowerShell/cmd/bash）启动
    #       记录的属主 SID 可能对不上，一旦被某种方式建过、换种方式再启动就 IllegalStateException 退出。
    #       每次启动前直接删掉重建最稳。java.io.tmpdir 走环境变量传，不走命令行拼接（三层 shell 嵌套
    #       引号极易出错）。
    $bootTmp = "$root\tools\.springboot-tmp"
    if (Test-Path $bootTmp) { Remove-Item -Recurse -Force $bootTmp }
    New-Item -ItemType Directory -Force -Path $bootTmp | Out-Null
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=$bootTmp"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "mvn spring-boot:run > `"$backendLog`" 2>&1" `
        -WorkingDirectory "$root\backend" -WindowStyle Minimized
    Write-Host "   等后端启动，大概几秒钟..."
    $ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port 8080) { $ready = $true; break }
    }
    if (-not $ready) {
        Write-Host "后端好像没起来，看日志：$backendLog" -ForegroundColor Red
        Write-Host "常见原因：没装 JDK17/Maven（新开个终端敲 mvn -v 看看），或者日志里能看到具体报错。" -ForegroundColor Red
        exit 1
    }
}

# ---------- 3) 后端隧道 ----------
Write-Host "3/5 后端公网隧道..." -ForegroundColor Cyan
$backendUrl = Ensure-Tunnel 8080 "backend"
if (-not $backendUrl) {
    Write-Host "后端隧道地址没读到，看 tools\tunnel-backend.log" -ForegroundColor Red
    exit 1
}
Write-Host "   $backendUrl"

# ---------- 4) 前端 dev server ----------
Write-Host "4/5 前端（Vite）..." -ForegroundColor Cyan
$envFile = "$root\frontend\.env.local"
$urlMarker = "$root\tools\.backend-url"
$oldUrl = (Get-Content $urlMarker -ErrorAction SilentlyContinue | Select-Object -First 1)
# 坑 3：PowerShell 5.1 的 Set-Content -Encoding utf8 会写 UTF-8 BOM，Vite 读 .env 时
#       BOM 会粘到第一个变量名上（变成 "﻿VITE_API_BASE_URL"），于是 VITE_API_BASE_URL
#       根本没被识别，前端 BASE_URL 回退到 localhost:8080，队友那边全是坏的。必须写不带 BOM。
[System.IO.File]::WriteAllText($envFile, "VITE_API_BASE_URL=$backendUrl`n", (New-Object System.Text.UTF8Encoding $false))
$backendUrl | Set-Content $urlMarker

$frontendLog = "$root\tools\frontend.log"
if ((Test-Port 5173) -and ($oldUrl -eq $backendUrl)) {
    Write-Host "   已经在跑了，跳过。"
} else {
    if (Test-Port 5173) {
        Write-Host "   后端地址变了，重启前端让它读到新地址..."
        Stop-PortProcess 5173
        Start-Sleep -Seconds 2
    }
    if (Test-Path $frontendLog) { Remove-Item $frontendLog -Force }
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm run dev > `"$frontendLog`" 2>&1" `
        -WorkingDirectory "$root\frontend" -WindowStyle Minimized
    Write-Host "   等前端启动..."
    $ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port 5173) { $ready = $true; break }
    }
    if (-not $ready) {
        Write-Host "前端没起来，看日志：$frontendLog（常见：frontend 目录没 npm install 过）" -ForegroundColor Red
        exit 1
    }
}

# ---------- 5) 前端隧道 ----------
Write-Host "5/5 前端公网隧道..." -ForegroundColor Cyan
$frontendUrl = Ensure-Tunnel 5173 "frontend"
if (-not $frontendUrl) {
    Write-Host "前端隧道地址没读到，看 tools\tunnel-frontend.log" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host " 全部就绪。把下面这个地址发到群里，队友浏览器打开就能用（什么都不用装）：" -ForegroundColor Green
Write-Host ""
Write-Host "   $frontendUrl" -ForegroundColor Yellow
Write-Host ""
Write-Host " （后端地址 $backendUrl —— 只是想用系统的队友用不到，前端会自己调；" -ForegroundColor DarkGray
Write-Host "   要在自己电脑改前端代码、本地跑 npm run dev 的队友，.env.local 里填这个）" -ForegroundColor DarkGray
Write-Host "==================================================================" -ForegroundColor Green
