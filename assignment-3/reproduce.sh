set -eu
cd "$(dirname "$0")"
bash build.sh
java -cp out tests.CorrectnessTests > results/tables/tests.txt
cat results/tables/tests.txt
java -cp out app.Main demo > results/tables/demo.txt
java -cp out benchmark.Benchmark
python3 scripts/plot_results.py
