#!/usr/bin/env bash
# Runs the game's 25 tests on a desktop JVM (no Android device needed), split across shards.
#   ./test.sh                 all 25 tests, in parallel (SHARDS=N to choose how many JVMs; at most 6)
#   FAST=1 ./test.sh          only the fast tier (run after every merge)
#   ./test.sh "maps"          only the tests whose names start with "maps" (in one JVM)
#   ./test.sh --list          the tests, the fast ones marked
#   ZC_SEED_SALT=101 ./test.sh   the same tests played out with other dice
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build/test
mkdir -p build/test
javac -nowarn -encoding UTF-8 -d build/test $(find tests/stubs tests/src -name '*.java') \
    $(find src -name '*.java' ! -name MainActivity.java ! -name Sound.java) 2>&1 | grep -v -e '^Note:' -e '^Picked up JAVA_TOOL_OPTIONS' || true
[ -f build/test/com/instantaxtion/zombiesandbox/GameTests.class ] || { echo "compile failed"; exit 1; }
run() { java -Djava.awt.headless=true -Xmx2g -cp build/test com.instantaxtion.zombiesandbox.GameTests "$@" 2>&1 | grep --line-buffered -v '^Picked up JAVA_TOOL_OPTIONS'; }
if [ "${1:-}" = "--list" ]; then
    run --list
    exit 0
fi
if [ $# -gt 0 ]; then
    ONLY="$1" run
    exit "${PIPESTATUS[0]}"
fi
N="${SHARDS:-$(nproc)}"
[ "$N" -gt 6 ] && N=6
pids=()
for ((i = 0; i < N; i++)); do
    SHARDS=$N SHARD=$i run > "build/test/shard$i.log" &
    pids+=($!)
done
status=0
for p in "${pids[@]}"; do wait "$p" || status=1; done
cat build/test/shard*.log | grep -v ' passed, ' || true
grep -h ' passed, ' build/test/shard*.log | awk '{p += $1; f += $3} END {print p " passed, " f " failed"}'
grep -q '^FAIL' build/test/shard*.log && status=1
exit $status
