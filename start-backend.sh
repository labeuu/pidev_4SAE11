#!/usr/bin/env bash
# ============================================================
# start-backend.sh  –  Start backend (ordered) + Angular frontend
# Run from the repo root:  bash start-backend.sh
#
# Set SKIP_FRONTEND=1 to start only the backend.
#
# Order rationale:
#   1. Eureka (Config Server registers with Eureka)
#   2. Config Server (native config for Offer, planning, Vendor, task, Subcontracting, …)
#   3. Keycloak Auth, API Gateway
#   4. Wave A: Eureka-backed gateway routes first (Offer, AImodel)
#   5. Wave B: other config-heavy services
#   6. task (after AI service; uses Config, Eureka, Feign to AIMODEL)
#   7. Wave D: remaining microservices
#   8. Frontend (ng serve) last — first compile can take a while
# ============================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACK="$ROOT/backEnd"
FRONTEND="$ROOT/frontend/smart-freelance-app"
FRONTEND_PORT="${FRONTEND_PORT:-4200}"
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

# Colour helpers
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
die()     { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ----------------------------------------------------------
# wait_for_port <port> <label> <timeout_seconds>
# Uses 127.0.0.1 (not localhost) to match services bound to IPv4.
# ----------------------------------------------------------
wait_for_port() {
  local port=$1 label=$2 timeout=${3:-90}
  local host="${READINESS_HOST:-127.0.0.1}"
  local base="http://${host}:${port}"
  local started
  started="$(date +%s)"
  info "Waiting for $label - probing ${base} (up to ${timeout}s)..."
  local elapsed=0
  local last_bucket=-1
  while [ "$elapsed" -lt "$timeout" ]; do
    local hp
    hp="$(curl -sS --max-time 2 "${base}/actuator/health" 2>/dev/null || true)"
    if echo "$hp" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' \
      && ! echo "$hp" | grep -q '"status"[[:space:]]*:[[:space:]]*"DOWN"'; then
      elapsed=$(($(date +%s) - started))
      info "$label - ready (Actuator health UP) - ${elapsed}s"
      return 0
    fi
    local ax
    ax="$(curl -sS --max-time 2 "${base}/actuator" 2>/dev/null || true)"
    if echo "$ax" | grep -q '_links'; then
      elapsed=$(($(date +%s) - started))
      info "$label - ready (Actuator index) - ${elapsed}s"
      return 0
    fi
    local root
    root="$(curl -sS --max-time 2 "${base}/" 2>/dev/null || true)"
    if echo "$root" | grep -qiE 'eureka|DS Replicas|spring-cloud-netflix'; then
      elapsed=$(($(date +%s) - started))
      info "$label - ready (Eureka UI) - ${elapsed}s"
      return 0
    fi
    if echo "$label" | grep -qiE 'angular|frontend' \
      && echo "$root" | grep -qi '<!DOCTYPE' \
      && echo "$root" | grep -qi '<html'; then
      elapsed=$(($(date +%s) - started))
      info "$label - ready (dev server HTML) - ${elapsed}s"
      return 0
    fi
    elapsed=$(($(date +%s) - started))
    local bucket=$((elapsed / 10))
    if [ "$elapsed" -ge 10 ] && [ "$bucket" -gt "$last_bucket" ]; then
      last_bucket=$bucket
      echo "       ... still waiting for $label (${elapsed}s elapsed)"
    fi
    sleep 1
  done
  warn "$label - no readiness signal on ${base} within ${timeout}s (check logs). Continuing."
  return 1
}

# ----------------------------------------------------------
# start_service <dir> <label> <log_file>
# ----------------------------------------------------------
start_service() {
  local dir=$1 label=$2 log=$3
  cd "$dir" || die "Directory not found: $dir"
  if [ -f "./mvnw" ]; then
    chmod +x ./mvnw 2>/dev/null || true
    ./mvnw spring-boot:run -q > "$LOG_DIR/$log" 2>&1 &
  elif [ -f "./mvnw.cmd" ]; then
    ./mvnw.cmd spring-boot:run -q > "$LOG_DIR/$log" 2>&1 &
  else
    mvn spring-boot:run -q > "$LOG_DIR/$log" 2>&1 &
  fi
  echo $! >> "$LOG_DIR/pids.txt"
  info "Started $label (PID $!, log -> logs/$log)"
  cd "$ROOT"
}

# ----------------------------------------------------------
# start_frontend_angular  –  npm run start (ng serve) in FRONTEND dir
# ----------------------------------------------------------
start_frontend_angular() {
  local dir=$1 log=$2
  if ! command -v npm >/dev/null 2>&1; then
    warn "npm not found in PATH — skipping frontend"
    return 1
  fi
  cd "$dir" || die "Directory not found: $dir"
  if [ ! -f package.json ]; then
    die "No package.json in $dir"
  fi
  if [ ! -d node_modules ]; then
    warn "node_modules missing — run: cd \"$dir\" && npm install"
  fi
  npm run start -- --host "${FRONTEND_HOST:-127.0.0.1}" --port "$FRONTEND_PORT" \
    > "$LOG_DIR/$log" 2>&1 &
  echo $! >> "$LOG_DIR/pids.txt"
  info "Started Angular dev server (PID $!, log -> logs/$log)"
  cd "$ROOT"
}

# ----------------------------------------------------------
# start_wave <message> then call start_* / wait_for_port externally
# ----------------------------------------------------------
wave_banner() {
  info "── $1 ──"
}

# Clear old PIDs
> "$LOG_DIR/pids.txt"

echo ""
echo "============================================================"
echo "  Smart Freelance – Backend + Frontend Startup"
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

# ── 5. Microservices (ordered waves) ─────────────────────

wave_banner "Wave A — Eureka / lb backends (Offer, AImodel)"
start_service "$BACK/Microservices/Offer" "Offer" "Offer.log"
start_service "$BACK/Microservices/AImodel" "AImodel" "AImodel.log"

wait_for_port 8082 "Offer" 120
wait_for_port 8095 "AImodel" 120

wave_banner "Wave B — Config clients (planning, Vendor, Subcontracting)"
start_service "$BACK/Microservices/planning" "planning" "planning.log"
start_service "$BACK/Microservices/Vendor" "Vendor" "Vendor.log"
start_service "$BACK/Microservices/Subcontracting" "Subcontracting" "Subcontracting.log"

wait_for_port 8081 "planning" 120
wait_for_port 8093 "Vendor" 120
wait_for_port 8099 "Subcontracting" 120

wave_banner "Wave C — task (after AI + config peers)"
start_service "$BACK/Microservices/task" "task" "task.log"
wait_for_port 8091 "task" 120

wave_banner "Wave D — remaining microservices"
REMAINING=(
  "user:8090"
  "Contract:8083"
  "Project:8084"
  "review:8085"
  "Portfolio:8086"
  "gamification:8088"
  "FreelanciaJob:8097"
  "ticket-service:8094"
  "Chat:8096"
  "Meeting:8101"
  "Notification:8098"
)

for entry in "${REMAINING[@]}"; do
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
info "Waiting for Wave D services…"
echo ""

for entry in "${REMAINING[@]}"; do
  svc="${entry%%:*}"
  port="${entry##*:}"
  dir="$BACK/Microservices/$svc"
  [ -d "$dir" ] && wait_for_port "$port" "$svc" 120
done

echo ""
if [ ! -d "$FRONTEND" ]; then
  warn "Frontend directory not found, skipping: $FRONTEND"
elif [ "${SKIP_FRONTEND:-0}" = "1" ]; then
  info "SKIP_FRONTEND=1 — not starting Angular dev server"
else
  wave_banner "Frontend — Angular (ng serve)"
  if start_frontend_angular "$FRONTEND" "frontend-angular.log"; then
    info "Waiting for dev server (first build may take several minutes)…"
    wait_for_port "$FRONTEND_PORT" "Angular dev server" 300
  fi
fi

echo ""
echo "============================================================"
echo "  All services started!"
echo ""
echo "  Eureka dashboard : http://localhost:8420"
echo "  API Gateway      : http://localhost:8078"
echo "  Keycloak Auth    : http://localhost:8079"
echo "  Config Server    : http://localhost:8888"
if [ "${SKIP_FRONTEND:-0}" != "1" ] && [ -d "$FRONTEND" ]; then
  echo "  Angular app      : http://${FRONTEND_HOST:-127.0.0.1}:$FRONTEND_PORT"
fi
echo ""
echo "  Logs in: $LOG_DIR"
echo "  PIDs in: $LOG_DIR/pids.txt"
echo ""
echo "  To stop everything:"
echo "    bash stop-backend.sh"
echo "============================================================"
