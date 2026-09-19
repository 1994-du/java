# start-services.ps1 - 启动 / 停止 F:\java Spring Boot 项目（直连云服务器 MySQL，不依赖本地数据库 / Docker）
#
# 用法（建议在自己的终端里跑，服务才能常驻）：
#   启动:  powershell -ExecutionPolicy Bypass -File F:\java\start-services.ps1
#   停止:  powershell -ExecutionPolicy Bypass -File F:\java\start-services.ps1 -Stop
#
# 说明：
#   - JDK 优先级：系统 JDK 21 (F:\JDK21) → $env:JAVA_HOME → 便携版 jdk 17
#   - 数据库直连云服务器（application.properties 中已配置 106.15.207.57），本机不再启动 MySQL
#   - 应用务必显式 --server.port=1234，否则宿主环境注入的 SERVER_PORT=54030 会抢占端口
#   - PowerShell 5.1 坑：Start-Process -WindowStyle Hidden 不能与输出重定向同用，
#     故 java 用 --logging.file.name= 写日志

param([switch]$Stop)

$ErrorActionPreference = 'SilentlyContinue'

# ---- 定位 JDK ----
$jdkCandidates = @(
    'F:\JDK21',
    $env:JAVA_HOME,
    'C:\Program Files\Java\latest\jdk-21',
    'C:\Users\17462\.workbuddy\binaries\jdk\jdk-17.0.20.1+1'
)
$jdk = $null
foreach ($c in $jdkCandidates) {
    if ($c -and (Test-Path "$c\bin\java.exe")) { $jdk = $c; break }
}
if (-not $jdk) { Write-Host 'ERROR: 未找到可用的 JDK'; exit 1 }

$JAR     = 'F:\java\target\springBootProject-0.0.1-SNAPSHOT.jar'
$APP_LOG = 'F:\java\.workbuddy\tmp-logs\app.log'

function Test-Port($p) {
    $l = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($l) { $l.OwningProcess } else { $null }
}

# ---- 停止 ----
if ($Stop) {
    $p1234 = Test-Port 1234
    if ($p1234) { taskkill /PID $p1234 /F | Out-Null; Write-Host "stopped PID $p1234 (port 1234)" }
    else { Write-Host '服务未在运行' }
    exit 0
}

# ---- 启动应用（按需）----
if (-not (Test-Port 1234)) {
    if (-not (Test-Path $JAR)) {
        Write-Host "ERROR: 未找到 $JAR，请先执行 mvnw clean package 打包"
        exit 1
    }
    Start-Process -FilePath "$jdk\bin\java.exe" -WindowStyle Hidden `
        -ArgumentList "-jar", "$JAR", "--server.port=1234", "--logging.file.name=$APP_LOG"
    for ($i = 0; $i -lt 45; $i++) { if (Test-Port 1234) { break }; Start-Sleep -Seconds 1 }
}

# ---- 状态 ----
Start-Sleep -Seconds 2
$h = (curl.exe -s -m 5 http://localhost:1234/actuator/health)
Write-Host "JDK 路径 : $jdk"
Write-Host "App  1234: $(if (Test-Port 1234) { 'UP' } else { 'DOWN' })  health=$h"
Write-Host ''
Write-Host '服务地址  : http://localhost:1234'
Write-Host '健康检查  : http://localhost:1234/actuator/health'
Write-Host '数据库    : 云服务器 106.15.207.57:3306/localhost（直连，无本地库）'
