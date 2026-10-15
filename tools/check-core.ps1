$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Work = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $Work | Out-Null
try {
    $Sources = Get-ChildItem (Join-Path $Root "core/src/main/kotlin"), (Join-Path $Root "core/src/check/kotlin") -Filter *.kt -Recurse | ForEach-Object { $_.FullName }
    & kotlinc @Sources -jvm-target 17 -include-runtime -d (Join-Path $Work "checks.jar")
    if ($LASTEXITCODE -ne 0) { throw "Kotlin compilation failed." }
    & java -jar (Join-Path $Work "checks.jar")
    if ($LASTEXITCODE -ne 0) { throw "Core checks failed." }
} finally {
    Remove-Item -Recurse -Force $Work
}
