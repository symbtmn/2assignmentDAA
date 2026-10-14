$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
& ./build.ps1
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp out tests.CorrectnessTests | Tee-Object results/tables/tests.txt
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp out app.Main demo | Set-Content results/tables/demo.txt
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp out benchmark.Benchmark
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python scripts/plot_results.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python scripts/write_report.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
