set -eu
cd "$(dirname "$0")"
mkdir -p out
find src tests -name '*.java' | sort > out/sources.txt
if command -v javac >/dev/null 2>&1; then
    javac --release 17 -Xlint:all -d out @out/sources.txt
else
    java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -Xlint:all -d out @out/sources.txt
fi
