param(
    [string]$SdkRoot = "",
    [string]$JdkHome = ""
)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

if (-not $SdkRoot) {
    if ($env:ANDROID_HOME -and (Test-Path -LiteralPath $env:ANDROID_HOME)) {
        $SdkRoot = $env:ANDROID_HOME
    } elseif ($env:ANDROID_SDK_ROOT -and (Test-Path -LiteralPath $env:ANDROID_SDK_ROOT)) {
        $SdkRoot = $env:ANDROID_SDK_ROOT
    } else {
        $SdkRoot = Join-Path (Split-Path $PSScriptRoot -Parent) "android-sdk"
    }
}
$SdkRoot = (Resolve-Path -LiteralPath $SdkRoot).Path

$BuildToolsVersion = "35.0.0"
$BuildTools = Join-Path $SdkRoot "build-tools\$BuildToolsVersion"
$AndroidJar = Join-Path $SdkRoot "platforms\android-34\android.jar"
$aapt2 = Join-Path $BuildTools "aapt2.exe"
$d8 = Join-Path $BuildTools "d8.bat"
$zipalign = Join-Path $BuildTools "zipalign.exe"
$apksigner = Join-Path $BuildTools "apksigner.bat"

if (-not $JdkHome) {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
        $JdkHome = $env:JAVA_HOME
    } else {
        $javaCandidates = @(
            "C:\Program Files\Java\jdk-25.0.3",
            "C:\Program Files\Java\jdk-21",
            "C:\Program Files\Java\jdk-17",
            "C:\Program Files\Eclipse Adoptium\jdk-17"
        )
        foreach ($candidate in $javaCandidates) {
            if (Test-Path -LiteralPath (Join-Path $candidate "bin\javac.exe")) {
                $JdkHome = $candidate
                break
            }
        }
    }
}
if (-not $JdkHome) {
    throw "JDK not found. Pass -JdkHome or set JAVA_HOME."
}
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$javac = Join-Path $JdkHome "bin\javac.exe"
$java = Join-Path $JdkHome "bin\java.exe"
$jar = Join-Path $JdkHome "bin\jar.exe"
$keytool = Join-Path $JdkHome "bin\keytool.exe"

foreach ($tool in @($aapt2, $d8, $zipalign, $apksigner, $AndroidJar, $javac, $java, $jar, $keytool)) {
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "Required tool not found: $tool`nInstall SDK platform 34 and build-tools $BuildToolsVersion."
    }
}

$root = $PSScriptRoot
$build = Join-Path $root "build"
$gen = Join-Path $build "gen"
$classesDir = Join-Path $build "classes"
$testClassesDir = Join-Path $build "test-classes"
$dexDir = Join-Path $build "dex"
$release = Join-Path $root "release"
$apk = Join-Path $release "假名打字练习.apk"
$keystore = Join-Path $root "keystore\kana.jks"
$keystorePassword = "KanaTyping2026"

Write-Host "SDK: $SdkRoot"
Write-Host "JDK: $JdkHome"

if (Test-Path -LiteralPath $build) {
    Remove-Item -LiteralPath $build -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $gen, $classesDir, $testClassesDir, $dexDir, $release | Out-Null

Write-Host "`n[1/6] Running game-logic tests"
$testSources = @(
    (Join-Path $root "java\com\kana\typing\Kana.java"),
    (Join-Path $root "java\com\kana\typing\Script.java"),
    (Join-Path $root "java\com\kana\typing\KanaLevel.java"),
    (Join-Path $root "java\com\kana\typing\KanaData.java"),
    (Join-Path $root "java\com\kana\typing\GameSession.java"),
    (Join-Path $root "tests\com\kana\typing\GameSessionTest.java")
)
& $javac --release 8 -Xlint:-options -encoding UTF-8 -d $testClassesDir $testSources
if ($LASTEXITCODE -ne 0) {
    throw "Test compilation failed."
}
& $java -cp $testClassesDir com.kana.typing.GameSessionTest
if ($LASTEXITCODE -ne 0) {
    throw "Game-logic tests failed."
}

Write-Host "`n[2/6] Compiling resources"
$resourcesZip = Join-Path $build "resources.zip"
$baseApk = Join-Path $build "base.apk"
& $aapt2 compile --dir (Join-Path $root "res") -o $resourcesZip
if ($LASTEXITCODE -ne 0) {
    throw "aapt2 compile failed."
}
& $aapt2 link -o $baseApk -I $AndroidJar --manifest (Join-Path $root "AndroidManifest.xml") --java $gen --auto-add-overlay $resourcesZip
if ($LASTEXITCODE -ne 0) {
    throw "aapt2 link failed."
}

Write-Host "`n[3/6] Compiling Java sources"
$javaSources = Get-ChildItem -Path (Join-Path $root "java"), $gen -Recurse -File -Filter *.java | ForEach-Object { $_.FullName }
& $javac --release 8 -nowarn -encoding UTF-8 -cp $AndroidJar -d $classesDir $javaSources
if ($LASTEXITCODE -ne 0) {
    throw "Android Java compilation failed."
}

Write-Host "`n[4/6] Dexing"
$classesJar = Join-Path $build "classes.jar"
Push-Location -LiteralPath $classesDir
try {
    & $jar cf $classesJar .
    if ($LASTEXITCODE -ne 0) {
        throw "jar failed."
    }
} finally {
    Pop-Location
}
& $d8 --lib $AndroidJar --min-api 23 --output $dexDir $classesJar
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath (Join-Path $dexDir "classes.dex"))) {
    throw "d8 failed."
}

Write-Host "`n[5/6] Assembling and signing APK"
$unsignedApk = Join-Path $build "unsigned.apk"
$alignedApk = Join-Path $build "aligned.apk"
Copy-Item -LiteralPath $baseApk -Destination $unsignedApk -Force
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($unsignedApk, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $entry = $archive.CreateEntry("classes.dex", [System.IO.Compression.CompressionLevel]::Optimal)
    $output = $entry.Open()
    $input = [System.IO.File]::OpenRead((Join-Path $dexDir "classes.dex"))
    try {
        $input.CopyTo($output)
    } finally {
        $input.Dispose()
        $output.Dispose()
    }
} finally {
    $archive.Dispose()
}

& $zipalign -f -p 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) {
    throw "zipalign failed."
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $keystore) | Out-Null
if (-not (Test-Path -LiteralPath $keystore)) {
    & $keytool -genkeypair -v -keystore $keystore -storepass $keystorePassword -keypass $keystorePassword -alias kana -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Kana Typing, OU=Development, O=Kana Typing, L=Local, ST=Local, C=CN"
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed."
    }
}

if (Test-Path -LiteralPath $apk) {
    Remove-Item -LiteralPath $apk -Force
}
$idsig = $apk + ".idsig"
if (Test-Path -LiteralPath $idsig) {
    Remove-Item -LiteralPath $idsig -Force
}
& $apksigner sign --ks $keystore --ks-key-alias kana --ks-pass "pass:$keystorePassword" --key-pass "pass:$keystorePassword" --out $apk $alignedApk
if ($LASTEXITCODE -ne 0) {
    throw "apksigner sign failed."
}
if (Test-Path -LiteralPath $idsig) {
    Remove-Item -LiteralPath $idsig -Force
}
& $apksigner verify --verbose $apk
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed."
}
& $zipalign -c -p 4 $apk
if ($LASTEXITCODE -ne 0) {
    throw "APK alignment check failed."
}

Write-Host "`n[6/6] Writing release metadata"
Copy-Item -LiteralPath (Join-Path $root "README.md") -Destination (Join-Path $release "使用说明.md") -Force
$screenshot = Join-Path $root "screenshot.png"
if (Test-Path -LiteralPath $screenshot) {
    Copy-Item -LiteralPath $screenshot -Destination (Join-Path $release "screenshot.png") -Force
}
$badgingPath = Join-Path $build "badging.txt"
& $aapt2 dump badging $apk | Set-Content -Encoding UTF8 -LiteralPath $badgingPath
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash
"$hash  假名打字练习.apk" | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $release "SHA256.txt")

$sizeKb = [math]::Round((Get-Item -LiteralPath $apk).Length / 1KB, 1)
Write-Host "`nDone: $apk ($sizeKb KB)"
Write-Host "SHA256: $hash"
Get-Content -LiteralPath $badgingPath -Encoding UTF8 | Select-String -Pattern '^(package:|minSdkVersion|targetSdkVersion|application-label:|launchable-activity:)' | ForEach-Object { $_.Line }
