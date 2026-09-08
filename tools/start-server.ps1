# 在"主机"这台电脑上运行：拉起本地 MySQL + 后端，并用 Cloudflare Tunnel 开一个公网地址，
# 团队其他人只要把前端的 VITE_API_BASE_URL 指向这个公网地址，注册/登录的数据就都落在这台电脑的数据库里。
#
# 用法：右键用 PowerShell 运行，或者:  powershell -ExecutionPolicy Bypass -File tools\start-server.ps1
#
# 说明：
#  - 这个隧道地址（xxx.trycloudflare.com）每次重新运行这个脚本都会换一个新的，不是固定的——
#    团队要用的话，每次都得把最新地址发一遍群里。想要固定地址得注册 Cloudflare 账号配置命名隧道，
#    这个脚本先不做，等确实需要稳定地址了再升级。
#  - 这台电脑不能关机、不能休眠，MySQL、后端、隧道三个进程也不能被关掉，不然团队那边就连不上了。

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.4\bin"
$mysqlData = "D:\tools\mysql-data"
$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

Write-Host "1/3 启动本地 MySQL..." -ForegroundColor Cyan
Start-Process -FilePath "$mysqlBin\mysqld.exe" -ArgumentList "--datadir=$mysqlData", "--port=3306" -WindowStyle Minimized
Start-Sleep -Seconds 4

Write-Host "2/3 启动后端（Spring Boot）..." -ForegroundColor Cyan
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "$root\backend" -WindowStyle Minimized
Write-Host "   等后端启动，大概几秒钟..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) {
        $ready = $true
        break
    }
}
if (-not $ready) {
    Write-Host "后端好像没起来，去看 backend 那个窗口的日志。" -ForegroundColor Red
    exit 1
}

Write-Host "3/3 开公网隧道..." -ForegroundColor Cyan
Start-Process -FilePath $cloudflared -ArgumentList "tunnel", "--url", "http://localhost:8080" `
    -RedirectStandardError "$root\tools\tunnel.log" -WindowStyle Minimized
Start-Sleep -Seconds 6

$line = Select-String -Path "$root\tools\tunnel.log" -Pattern "https://.*trycloudflare\.com" | Select-Object -First 1
if ($line) {
    $url = ($line.Matches[0].Value)
    Write-Host ""
    Write-Host "全部启动完成。团队成员在自己电脑的 frontend/.env.local 里写：" -ForegroundColor Green
    Write-Host "VITE_API_BASE_URL=$url" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "（这个地址每次重启脚本都会变，发到群里的时候记得更新）"
} else {
    Write-Host "隧道地址还没读到，稍等几秒后自己看 tools\tunnel.log" -ForegroundColor Yellow
}
