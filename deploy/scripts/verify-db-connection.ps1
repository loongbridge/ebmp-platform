<#
    EBMP · 启动后数据库连接验证（Windows / PowerShell 5.1+）

    验证顺序：
        1. 后端就绪与健康检查        GET  /api/health
        2. 数据库读（账号表）        POST /api/auth/login
        3. 鉴权 + 用户表读取         GET  /api/auth/me
        4. 业务多表聚合读            GET  /api/dashboard/overview
        5. （可选）MySQL 直连表数量   本机存在 mysql 客户端且设置了 MYSQL_HOST 时执行

    用法：
        powershell -ExecutionPolicy Bypass -File verify-db-connection.ps1
        powershell -ExecutionPolicy Bypass -File verify-db-connection.ps1 -BaseUrl http://10.0.0.8:8080 -WaitSeconds 90

    参数 / 环境变量：
        -BaseUrl  / EBMP_BASE_URL   默认 http://127.0.0.1:8080
        -Username / EBMP_USER       默认 admin
        -Password / EBMP_PASSWORD   默认 Admin@123
        MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE（设置 HOST 后追加直连校验）

    退出码：0 全部通过；1 存在失败项
#>
param(
    [string]$BaseUrl,
    [string]$Username,
    [string]$Password,
    [int]$WaitSeconds = 60
)

$ErrorActionPreference = 'Continue'

if (-not $BaseUrl) { if ($env:EBMP_BASE_URL) { $BaseUrl = $env:EBMP_BASE_URL } else { $BaseUrl = 'http://127.0.0.1:8080' } }
if (-not $Username) { if ($env:EBMP_USER) { $Username = $env:EBMP_USER } else { $Username = 'admin' } }
if (-not $Password) { if ($env:EBMP_PASSWORD) { $Password = $env:EBMP_PASSWORD } else { $Password = 'Admin@123' } }
$BaseUrl = $BaseUrl.TrimEnd('/')

$script:failed = 0
$script:token = $null
$rows = New-Object System.Collections.ArrayList

# 统一请求：显式按 UTF-8 解码，避免 PowerShell 5.1 无 charset 时按 ISO-8859-1 解析中文乱码
function Invoke-Json {
    param(
        [string]$Uri,
        [string]$Method = 'GET',
        [hashtable]$Headers = @{},
        [string]$JsonBody = $null
    )
    $params = @{
        Uri             = $Uri
        Method          = $Method
        TimeoutSec      = 10
        UseBasicParsing = $true
    }
    if ($Headers.Count -gt 0) { $params['Headers'] = $Headers }
    if ($JsonBody) {
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($JsonBody)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    try {
        $resp = Invoke-WebRequest @params
        $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    } catch {
        $ex = $_.Exception
        if ($ex.Response) {
            $stream = $ex.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
            $text = $reader.ReadToEnd()
            $reader.Close()
        } else {
            throw
        }
    }
    if ([string]::IsNullOrWhiteSpace($text)) { throw '响应为空' }
    return ($text | ConvertFrom-Json)
}

function Add-Result {
    param([string]$Name, [bool]$Ok, [int]$Ms, [string]$Detail)
    if ($Ok) {
        Write-Host ("  [PASS] {0,-26} {1,6} ms  {2}" -f $Name, $Ms, $Detail) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0,-26} {1,6} ms  {2}" -f $Name, $Ms, $Detail) -ForegroundColor Red
        $script:failed++
    }
    $flag = 'FAIL'
    if ($Ok) { $flag = 'PASS' }
    [void]$rows.Add([pscustomobject]@{ 检查项 = $Name; 结果 = $flag; 耗时 = "$Ms ms"; 说明 = $Detail })
}

Write-Host ''
Write-Host '=======================================================' -ForegroundColor Cyan
Write-Host '  EBMP 数据库连接验证' -ForegroundColor Cyan
Write-Host "  后端地址 : $BaseUrl"
Write-Host "  验证账号 : $Username"
if ($env:MYSQL_HOST) {
    Write-Host "  数据库   : MySQL @ $($env:MYSQL_HOST)"
} else {
    Write-Host '  数据库   : H2（内置文件库，通过接口间接验证）'
}
Write-Host '=======================================================' -ForegroundColor Cyan
Write-Host ''

# ---------- 0. 等待后端就绪 ----------
Write-Host "[等待] 后端就绪（最多 $WaitSeconds 秒）..." -ForegroundColor Yellow
$ready = $false
$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ((Get-Date) -lt $deadline) {
    try {
        $probe = Invoke-Json -Uri "$BaseUrl/api/health"
        if ($probe.code -eq 0) { $ready = $true; break }
    } catch { }
    Start-Sleep -Milliseconds 800
}
if (-not $ready) {
    Write-Host '  [FAIL] 后端未在限定时间内就绪：请确认进程已启动、端口可访问' -ForegroundColor Red
    Write-Host ''
    Write-Host '结论: 验证中止（后端不可达）' -ForegroundColor Red
    exit 1
}
Write-Host '  [ OK ] 后端已就绪' -ForegroundColor Green
Write-Host ''

# ---------- 1. 健康检查 ----------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $r = Invoke-Json -Uri "$BaseUrl/api/health"
    $sw.Stop()
    $ok = ($r.code -eq 0 -and $r.data.status -eq 'UP')
    Add-Result 'GET /api/health' $ok $sw.ElapsedMilliseconds "status=$($r.data.status) platform=$($r.data.platform)"
} catch {
    $sw.Stop()
    Add-Result 'GET /api/health' $false $sw.ElapsedMilliseconds $_.Exception.Message
}

# ---------- 2. 登录（读 sys_user / sys_role 等表） ----------
# 首次启动时 DataSeeder 写入账号可能存在秒级延迟，失败时短重试
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
$loginOk = $false
$loginDetail = ''
for ($attempt = 1; $attempt -le 5; $attempt++) {
    try {
        $r = Invoke-Json -Uri "$BaseUrl/api/auth/login" -Method 'POST' -JsonBody $loginBody
        if ($r.code -eq 0 -and $r.data.token) {
            $script:token = $r.data.token
            $loginOk = $true
            $loginDetail = "user=$($r.data.user.username) roles=$($r.data.user.roles -join ',')"
            break
        }
        $loginDetail = "code=$($r.code) message=$($r.message)"
    } catch {
        $loginDetail = $_.Exception.Message
    }
    Start-Sleep -Seconds 2
}
$sw.Stop()
Add-Result 'POST /api/auth/login' $loginOk $sw.ElapsedMilliseconds $loginDetail

# ---------- 3. 鉴权 + 用户表读取 ----------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if (-not $script:token) {
    $sw.Stop()
    Add-Result 'GET /api/auth/me' $false 0 '登录未通过，跳过'
} else {
    try {
        $r = Invoke-Json -Uri "$BaseUrl/api/auth/me" -Headers @{ Authorization = "Bearer $($script:token)" }
        $sw.Stop()
        Add-Result 'GET /api/auth/me' ($r.code -eq 0) $sw.ElapsedMilliseconds "user=$($r.data.username) dept=$($r.data.deptName)"
    } catch {
        $sw.Stop()
        Add-Result 'GET /api/auth/me' $false $sw.ElapsedMilliseconds $_.Exception.Message
    }
}

# ---------- 4. 业务多表聚合读 ----------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if (-not $script:token) {
    $sw.Stop()
    Add-Result 'GET /api/dashboard/overview' $false 0 '登录未通过，跳过'
} else {
    try {
        $r = Invoke-Json -Uri "$BaseUrl/api/dashboard/overview" -Headers @{ Authorization = "Bearer $($script:token)" }
        $sw.Stop()
        Add-Result 'GET /api/dashboard/overview' ($r.code -eq 0) $sw.ElapsedMilliseconds '订单/客户/库存/待办等聚合指标读取成功'
    } catch {
        $sw.Stop()
        Add-Result 'GET /api/dashboard/overview' $false $sw.ElapsedMilliseconds $_.Exception.Message
    }
}

# ---------- 5. 可选：MySQL 直连校验 ----------
if ($env:MYSQL_HOST) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysqlCmd) {
        $sw.Stop()
        Add-Result 'MySQL 直连表数量' $false 0 '未找到 mysql 客户端，跳过直连校验'
    } else {
        $port = '3306'; if ($env:MYSQL_PORT) { $port = $env:MYSQL_PORT }
        $db = 'ebmp'; if ($env:MYSQL_DATABASE) { $db = $env:MYSQL_DATABASE }
        $sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db' AND table_type='BASE TABLE';"
        try {
            $out = & mysql -h $env:MYSQL_HOST -P $port -u $env:MYSQL_USER "-p$($env:MYSQL_PASSWORD)" -N -B -e $sql 2>&1
            $sw.Stop()
            $count = ($out | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1)
            if ($count) {
                $ok = ([int]$count -ge 30)
                Add-Result 'MySQL 直连表数量' $ok $sw.ElapsedMilliseconds "库=$db 表数=$count（期望 34）"
            } else {
                Add-Result 'MySQL 直连表数量' $false $sw.ElapsedMilliseconds ($out -join ' ')
            }
        } catch {
            $sw.Stop()
            Add-Result 'MySQL 直连表数量' $false $sw.ElapsedMilliseconds $_.Exception.Message
        }
    }
}

# ---------- 汇总 ----------
Write-Host ''
Write-Host '-------------------------------------------------------' -ForegroundColor Cyan
$rows | Format-Table -AutoSize
Write-Host '-------------------------------------------------------' -ForegroundColor Cyan

if ($script:failed -eq 0) {
    Write-Host '结论: 数据库连接正常，后端可正常读写数据库' -ForegroundColor Green
    exit 0
} else {
    Write-Host "结论: 存在 $($script:failed) 项失败，请检查后端日志与数据库配置" -ForegroundColor Red
    exit 1
}
