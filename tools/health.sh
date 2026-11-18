#!/usr/bin/env bash
# A health check of a real game, run on every merge: two minutes (simulated) of Metropolis and the Archipelago at the
# Default size, a small outbreak ten seconds in, the dice fixed. Counts vehicles stuck, driven into each other and
# spinning on the spot, police idle far from any station, police numbers, response trip times and the simulation's
# speed, writes them to build/health/health.txt and compares them with tools/health_baseline.txt (recorded on 28.0):
# OK, or WARN where a number is much worse.
#   tools/health.sh              run and compare
#   tools/health.sh --baseline   run and record the numbers as the new baseline
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/health
rm -rf build/health/classes
mkdir -p build/health/classes
javac -nowarn -encoding UTF-8 -d build/health/classes $(find tests/stubs tests/src -name '*.java') \
    $(find src -name '*.java' ! -name MainActivity.java ! -name Sound.java) 2>&1 | grep -v -e '^Note:' -e '^Picked up JAVA_TOOL_OPTIONS' || true
[ -f build/health/classes/com/instantaxtion/zombiesandbox/Health.class ] || { echo "compile failed"; exit 1; }
java -Djava.awt.headless=true -Xmx3g -cp build/health/classes com.instantaxtion.zombiesandbox.Health run build/health/health.txt 2>&1 \
    | grep -v '^Picked up JAVA_TOOL_OPTIONS'
if [ "${1:-}" = "--baseline" ]; then
    { echo "# tools/health.sh numbers, recorded $(date -u +%Y-%m-%d) on $(git rev-parse --short HEAD 2>/dev/null || echo '?')"; cat build/health/health.txt; } > tools/health_baseline.txt
    echo "baseline written to tools/health_baseline.txt"
    exit 0
fi
echo
java -cp build/health/classes com.instantaxtion.zombiesandbox.Health compare build/health/health.txt tools/health_baseline.txt 2>&1 \
    | grep -v '^Picked up JAVA_TOOL_OPTIONS'
