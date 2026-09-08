# 在"主机"这台电脑上运行：拉起本地 MySQL + 后端，并用 Cloudflare Tunnel 开一个公网地址，
# 团队其他人只要把前端的 VITE_API_BASE_URL 指向这个公网地址，注册/登录的数据就都落在这台电脑的数据库里。
#
# 用法：直接双击同目录下的 start-server.bat（这个 .ps1 本身双击不会真的执行，Windows 默认不这么处理 .ps1）。
#   命令行等价写法：powershell -ExecutionPolicy Bypass -File tools\start-server.ps1
#
# 这个脚本重复运行是安全的——已经在跑的部分会跳过，不会启动出一堆重复进程互相抢端口。
#
# 说明：
#  - 这个隧道地址（xxx.trycloudflare.com）每次真正新开一个隧道都会换一个新的，不是固定的——
#    团队要用的话，每次都得把最新地址发一遍群里。想要固定地址得注册 Cloudflare 账号配置命名隧道，
#    这个脚本先不做，等确实需要稳定地址了再升级。
#  - 这台电脑不能关机、不能休眠，MySQL、后端、隧道三个进程也不能被关掉，不然团队那边就连不上了。

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.4\bin"
$mysqlData = "D:\tools\mysql-data"
$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

function Test-Port($port) {
    Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
}

Write-Host "1/3 本地 MySQL..." -ForegroundColor Cyan
if (Test-Port 3306) {
    Write-Host "   已经在跑了，跳过。"
} else {
    Start-Process -FilePath "$mysqlBin\mysqld.exe" -ArgumentList "--datadir=$mysqlData", "--port=3306" -WindowStyle Minimized
    Start-Sleep -Seconds 4
}

Write-Host "2/3 后端（Spring Boot）..." -ForegroundColor Cyan
if (Test-Port 8080) {
    Write-Host "   已经在跑了，跳过。"
} else {
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "$root\backend" -WindowStyle Minimized
    Write-Host "   等后端启动，大概几秒钟..."
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port 8080) { $ready = $true; break }
    }
    if (-not $ready) {
        Write-Host "后端好像没起来。常见原因：没装 JDK17/Maven，或者 mvn 不在 PATH 里——" -ForegroundColor Red
        Write-Host "打开一个新的 PowerShell 窗口敲 mvn -v 看看有没有反应，装好了重开一个终端再跑这个脚本。" -ForegroundColor Red
        exit 1
    }
}

Write-Host "3/3 公网隧道..." -ForegroundColor Cyan
$tunnelLog = "$root\tools\tunnel.log"
$existing = Get-Process cloudflared -ErrorAction SilentlyContinue
if ($existing -and (Test-Path $tunnelLog)) {
    Write-Host "   隧道已经开着了，读现成的地址。"
} else {
    if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force }
    Start-Process -FilePath $cloudflared -ArgumentList "tunnel", "--url", "http://localhost:8080" `
        -RedirectStandardError $tunnelLog -WindowStyle Minimized
    Start-Sleep -Seconds 6
}

$line = Select-String -Path $tunnelLog -Pattern "https://.*trycloudflare\.com" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($line) {
    $url = ($line.Matches[0].Value)
    Write-Host ""
    Write-Host "全部就绪。团队成员在自己电脑的 frontend/.env.local 里写：" -ForegroundColor Green
    Write-Host "VITE_API_BASE_URL=$url" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "（只有真正新开隧道时地址才会变；这次如果上面显示「已经开着了」，地址跟上次一样）"
} else {
    Write-Host "隧道地址还没读到，稍等几秒后自己看 tools\tunnel.log" -ForegroundColor Yellow
}
