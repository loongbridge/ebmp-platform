# 手工构建并签名 EBMP 移动端 APK（无需 Gradle / Android Studio）
# 依赖：Android SDK build-tools 34.0.0 + platforms/android-34 + JDK
# 说明：aapt2 无法处理含中文与 & 的工作区路径，因此中间产物统一放到 ASCII 临时目录编译。
param(
    [string]$SdkRoot = "$env:LOCALAPPDATA\Temp\android-sdk",
    [string]$BuildToolsVersion = "34.0.0",
    [string]$Platform = "android-34",
    [string]$OutDir = "$PSScriptRoot\build",
    [string]$KeyStore = "$PSScriptRoot\keystore\ebmp-release.jks",
    [string]$StorePass = "ebmp123456"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot   # mobile/
$dist = Join-Path $root "dist"
$bt = Join-Path $SdkRoot "build-tools\$BuildToolsVersion"
$androidJar = Join-Path $SdkRoot "platforms\$Platform\android.jar"

function Assert-File($path, $hint) {
    if (-not (Test-Path $path)) { throw "缺少 $path $hint" }
}

Write-Host "[1/9] 检查构建工具" -ForegroundColor Cyan
Assert-File $bt "Android build-tools $BuildToolsVersion"
Assert-File $androidJar "Android platform $Platform"
Assert-File (Join-Path $dist "index.html") "移动端 H5 产物，请先执行 npm run build"

Write-Host "[2/9] 生成启动图标" -ForegroundColor Cyan
node (Join-Path $PSScriptRoot "tools\make-icon.mjs")
if ($LASTEXITCODE -ne 0) { throw "图标生成失败" }

Write-Host "[3/9] 准备 ASCII 临时构建目录" -ForegroundColor Cyan
$stage = Join-Path $env:TEMP "ebmp-apk-stage"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item (Join-Path $PSScriptRoot "AndroidManifest.xml") $stage -Force
Copy-Item (Join-Path $PSScriptRoot "res") $stage -Recurse -Force
Copy-Item (Join-Path $PSScriptRoot "src") $stage -Recurse -Force
$assets = Join-Path $stage "assets"
$www = Join-Path $assets "www"
New-Item -ItemType Directory -Path $www -Force | Out-Null
Copy-Item "$dist\*" $www -Recurse -Force

$work = Join-Path $stage "work"
New-Item -ItemType Directory -Path $work | Out-Null
$compiled = Join-Path $work "resources.zip"
$gen = Join-Path $work "gen"
$unsigned = Join-Path $work "app-unsigned.apk"
New-Item -ItemType Directory -Path $gen | Out-Null

Write-Host "[4/9] 编译并链接资源" -ForegroundColor Cyan
& (Join-Path $bt "aapt2.exe") compile -o $compiled --dir (Join-Path $stage "res")
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile 失败" }

& (Join-Path $bt "aapt2.exe") link -o $unsigned -I $androidJar `
    --manifest (Join-Path $stage "AndroidManifest.xml") `
    --java $gen --auto-add-overlay $compiled
if ($LASTEXITCODE -ne 0) { throw "aapt2 link 失败" }

Write-Host "[5/9] 编译 Java 源码" -ForegroundColor Cyan
$classes = Join-Path $work "classes"
New-Item -ItemType Directory -Path $classes | Out-Null
$sources = @()
$sources += Get-ChildItem (Join-Path $stage "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$sources += Get-ChildItem $gen -Recurse -Filter *.java | ForEach-Object { $_.FullName }

# javac 会把过时 API 提示写到 stderr，PowerShell 的 Stop 策略会把它当成异常，这里临时放宽
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& javac -encoding UTF-8 --release 8 -Xlint:-options -classpath $androidJar -d $classes @sources 2>&1 | ForEach-Object { Write-Host $_ }
$ErrorActionPreference = $prevEap
if ($LASTEXITCODE -ne 0) { throw "javac 编译失败" }


Write-Host "[6/9] d8 生成 classes.dex" -ForegroundColor Cyan
$dexDir = Join-Path $work "dex"
New-Item -ItemType Directory -Path $dexDir | Out-Null
$classFiles = Get-ChildItem $classes -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& (Join-Path $bt "d8.bat") --lib $androidJar --min-api 21 --output $dexDir @classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 失败" }

Write-Host "[7/9] 写入 assets 并合并 classes.dex" -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$dex = Join-Path $dexDir "classes.dex"
Assert-File $dex "d8 未生成 classes.dex"
# aapt2 的 -A 会在 Windows 上把 assets 条目写成反斜杠（WebView 将找不到 www/index.html），
# 因此不使用 -A，改为在这里以正斜杠条目名写入，同时保持 aapt2 生成的 resources.arsc 原样（未压缩）。
$assetsRoot = (Get-Item $assets).FullName
$zip = [System.IO.Compression.ZipFile]::Open($unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    foreach ($f in Get-ChildItem $assets -Recurse -File) {
        $rel = $f.FullName.Substring($assetsRoot.Length + 1).Replace('\', '/')
        $entryName = "assets/$rel"
        $entry = $zip.GetEntry($entryName)
        if ($entry) { $entry.Delete() }
        $newEntry = $zip.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Optimal)
        $in = [System.IO.File]::OpenRead($f.FullName)
        $out = $newEntry.Open()
        try { $in.CopyTo($out) } finally { $out.Dispose(); $in.Dispose() }
    }
    $dexEntry = $zip.CreateEntry("classes.dex", [System.IO.Compression.CompressionLevel]::Optimal)
    $dexOut = $dexEntry.Open()
    try {
        $bytes = [System.IO.File]::ReadAllBytes($dex)
        $dexOut.Write($bytes, 0, $bytes.Length)
    } finally { $dexOut.Dispose() }
} finally { $zip.Dispose() }

$withDex = $unsigned

Write-Host "[8/9] zipalign 对齐" -ForegroundColor Cyan
$aligned = Join-Path $work "app-aligned.apk"
& (Join-Path $bt "zipalign.exe") -f -p 4 $withDex $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign 失败" }

Write-Host "[9/9] 生成签名并签名 APK" -ForegroundColor Cyan
if (-not (Test-Path $KeyStore)) {
    New-Item -ItemType Directory -Path (Split-Path -Parent $KeyStore) -Force | Out-Null
    & keytool -genkeypair -keystore $KeyStore -alias ebmp -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass $StorePass -keypass $StorePass `
        -dname "CN=EBMP, OU=IT, O=EBMP, L=Hangzhou, ST=Zhejiang, C=CN"
    if ($LASTEXITCODE -ne 0) { throw "keytool 生成签名失败" }
}

$release = Join-Path $work "EBMP-mobile-1.0.0.apk"
# apksigner.bat 经 cmd 解析参数，无法处理密钥库路径中的 &，先复制到 ASCII 临时目录
$signKs = Join-Path $work "signing.jks"
Copy-Item $KeyStore $signKs -Force
& (Join-Path $bt "apksigner.bat") sign --ks $signKs --ks-key-alias ebmp `
    --ks-pass "pass:$StorePass" --key-pass "pass:$StorePass" `
    --out $release $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner 签名失败" }

& (Join-Path $bt "apksigner.bat") verify --print-certs $release
if ($LASTEXITCODE -ne 0) { throw "APK 签名校验失败" }

& (Join-Path $bt "aapt2.exe") dump badging $release | Select-Object -First 6

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$final = Join-Path $OutDir "EBMP-mobile-1.0.0.apk"
Copy-Item $release $final -Force

Write-Host ""
Write-Host "构建完成：$final" -ForegroundColor Green
Write-Host ("大小：{0:N2} MB" -f ((Get-Item $final).Length / 1MB)) -ForegroundColor Green
