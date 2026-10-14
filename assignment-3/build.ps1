$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem src, tests -Filter *.java -Recurse | ForEach-Object { '"' + $_.FullName + '"' }
$sources | Set-Content -Encoding ASCII out/sources.txt
javac --release 17 -Xlint:all -d out '@out/sources.txt'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
