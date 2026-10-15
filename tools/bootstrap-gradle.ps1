$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Version = "8.13"
$ExpectedHash = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Install JDK 17+ and add java to PATH first." }
$GradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $HOME ".gradle" }
$Cache = Join-Path $GradleHome "aerodrive-bootstrap"
New-Item -ItemType Directory -Path $Cache -Force | Out-Null
$Zip = Join-Path $Cache "gradle-$Version-bin.zip"
if (-not (Test-Path $Zip)) {
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$Version-bin.zip" -OutFile "$Zip.part"
    Move-Item -Path "$Zip.part" -Destination $Zip -Force
}
if ((Get-FileHash -Path $Zip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $ExpectedHash) {
    throw "Gradle checksum mismatch. Delete $Zip and retry."
}
$Work = Join-Path ([System.IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $Work | Out-Null
try {
    Expand-Archive -Path $Zip -DestinationPath $Work
    $Project = Join-Path $Work "wrapper-project"
    New-Item -ItemType Directory -Path $Project | Out-Null
    'rootProject.name = "wrapper-bootstrap"' | Set-Content (Join-Path $Project "settings.gradle.kts") -Encoding Ascii
    & (Join-Path $Work "gradle-$Version/bin/gradle.bat") --no-daemon -p $Project wrapper --gradle-version $Version --distribution-type bin --gradle-distribution-sha256-sum $ExpectedHash
    if ($LASTEXITCODE -ne 0) { throw "Gradle wrapper setup failed with exit code $LASTEXITCODE." }
    Copy-Item (Join-Path $Project "gradlew") $Root -Force
    Copy-Item (Join-Path $Project "gradlew.bat") $Root -Force
    $Wrapper = Join-Path $Root "gradle/wrapper"
    New-Item -ItemType Directory -Path $Wrapper -Force | Out-Null
    Copy-Item (Join-Path $Project "gradle/wrapper/*") $Wrapper -Force
    Write-Host "Gradle wrapper installed. Open $Root in Android Studio."
} finally {
    Remove-Item $Work -Recurse -Force
}
