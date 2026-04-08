#!/usr/bin/env bash
# ============================================================
# start-backend.sh  –  Start all backend services in order
# Run from the repo root:  bash start-backend.sh
# ============================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACK="$ROOT/backEnd"
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

# Colour helpers
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
die()     { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ----------------------------------------------------------
# wait_for_port <port> <label> <timeout_seconds>
# ----------------------------------------------------------
wait_for_port() {
  local port=$1 label=$2 timeout=${3:-90}
  info "Waiting for $label on port $port (up to ${timeout}s)…"
  local elapsed=0
  while ! curl -s "http://localhost:$port/actuator/health" 2>/dev/null | grep -q '"UP"' && \
        ! curl -s "http://localhost:$port/" 2>/dev/null | grep -qiE 'eureka|html|OK' ; do
    sleep 3
    elapsed=$((elapsed + 3))
    if [ $elapsed -ge $timeout ]; then
      warn "$label did not become ready in ${timeout}s – continuing anyway"
      return 1
    fi
  done
  info "$label is UP"
}

# ----------------------------------------------------------
# start_service <dir> <label> <log_file>
# ----------------------------------------------------------
start_service() {
  local dir=$1 label=$2 log=$3
  info "Starting $label …  (log → logs/$log)"
  cd "$dir" || die "Directory not found: $dir"
  mvn spring-boot:run -q > "$LOG_DIR/$log" 2>&1 &
  echo $! >> "$LOG_DIR/pids.txt"
  cd "$ROOT"
}

# Clear old PIDs
> "$LOG_DIR/pids.txt"

echo ""
echo "============================================================"
echo "  Smart Freelance – Backend Startup"
echo "============================================================"
echo ""

# ── 1. Eureka (port 8420) ─────────────────────────────────
start_service "$BACK/Eureka" "Eureka" "eureka.log"
wait_for_port 8420 "Eureka" 120

# ── 2. Config Server (port 8888) ─────────────────────────
start_service "$BACK/ConfigServer" "Config Server" "config-server.log"
wait_for_port 8888 "Config Server" 90

# ── 3. Keycloak Auth service (port 8079) ─────────────────
#    NOTE: Keycloak itself (port 8080/8421) must already be running.
start_service "$BACK/KeyCloak" "Keycloak Auth" "keycloak-auth.log"
wait_for_port 8079 "Keycloak Auth" 90

# ── 4. API Gateway (port 8078) ───────────────────────────
start_service "$BACK/apiGateway" "API Gateway" "api-gateway.log"
wait_for_port 8078 "API Gateway" 90

# ── 5. Microservices (all in parallel) ───────────────────
info "Starting all microservices in parallel…"

SERVICES=(
  "user:8090"
  "planning:8081"
  "Offer:8082"
  "Contract:8083"
  "Project:8084"
  "review:8085"
  "Portfolio:8086"
  "Notification:8098"
  "task:8091"
)

for entry in "${SERVICES[@]}"; do
  svc="${entry%%:*}"
  port="${entry##*:}"
  dir="$BACK/Microservices/$svc"
  if [ -d "$dir" ]; then
    start_service "$dir" "$svc" "${svc}.log"
  else
    warn "Directory not found, skipping: $dir"
  fi
done

echo ""
info "All services launched. Waiting for microservices to come up…"
echo ""

for entry in "${SERVICES[@]}"; do
  svc="${entry%%:*}"
  port="${entry##*:}"
  dir="$BACK/Microservices/$svc"
  [ -d "$dir" ] && wait_for_port "$port" "$svc" 120
done

echo ""
echo "============================================================"
echo "  All services started!"
echo ""
echo "  Eureka dashboard : http://localhost:8420"
echo "  API Gateway      : http://localhost:8078"
echo "  Keycloak Auth    : http://localhost:8079"
echo "  Config Server    : http://localhost:8888"
echo ""
echo "  Logs in: $LOG_DIR"
echo "  PIDs in: $LOG_DIR/pids.txt"
echo ""
echo "  To stop everything:"
echo "    bash stop-backend.sh"
echo "============================================================"
