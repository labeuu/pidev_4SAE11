#!/usr/bin/env bash
# ============================================================
# stop-backend.sh  –  Stop all backend services started by start-backend.sh
#
# Kills PIDs in reverse start order so dependents stop before
# infrastructure (Angular dev server first if started last, then
# microservices, gateway; Eureka last).
# ============================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PIDS_FILE="$LOG_DIR/pids.txt"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

if [ ! -f "$PIDS_FILE" ]; then
  echo -e "${RED}[ERROR]${NC} No PID file found at $PIDS_FILE"
  exit 1
fi

pids=()
while IFS= read -r line || [ -n "$line" ]; do
  pid="${line//$'\r'/}"
  pid="${pid// /}"
  [ -n "$pid" ] && pids+=("$pid")
done < "$PIDS_FILE"

if [ ${#pids[@]} -eq 0 ]; then
  echo -e "${YELLOW}[WARN]${NC} No PIDs in $PIDS_FILE"
  rm -f "$PIDS_FILE"
  exit 0
fi

echo "Stopping backend services (reverse startup order)…"

for (( idx=${#pids[@]}-1 ; idx>=0 ; idx-- )); do
  pid="${pids[idx]}"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null
    echo -e "${GREEN}[TERM]${NC} PID $pid"
  fi
done

rm -f "$PIDS_FILE"
echo "Done."
