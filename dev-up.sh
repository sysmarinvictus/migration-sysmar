#!/usr/bin/env bash
# Dev-stack launcher for the NEW app: db (check/start) + backend (:8090) + frontend (:5173).
# The legacy GeneXus app stays on :8080 (managed separately on the Windows host) — untouched.
#
# Usage:
#   ./dev-up.sh                # DB = live non-prod snapshot (host.docker.internal:5432) — real data
#   ./dev-up.sh snapshot       # same as above (explicit)
#   ./dev-up.sh local          # DB = local Docker postgres (docker compose), schema built by Flyway
#
# Stop everything with ./dev-down.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="${1:-snapshot}"
BE_PORT=8090
FE_PORT=5173
STATE="$ROOT/.devstack"; mkdir -p "$STATE"
JAR="$ROOT/backend/target/receituario-backend.jar"
ENV_FILE="$ROOT/backend/.env"

# Secrets (dev-only). JWT secret matches application-local.yml default; CERT key satisfies the
# fail-closed cert converter bean (v1 never writes a cert password, so a throwaway key is fine).
export JWT_SECRET="${JWT_SECRET:-dev-local-insecure-secret-change-me-please-32b}"
export CERT_ENC_KEY="${CERT_ENC_KEY:-$(printf '%032d' 0 | base64)}"

log()  { printf '\033[36m[dev-up]\033[0m %s\n' "$*"; }
die()  { printf '\033[31m[dev-up] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# --- stop anything we previously started (idempotent) ---
"$ROOT/dev-down.sh" --quiet 2>/dev/null || true

# --- resolve DB target ---
case "$MODE" in
  snapshot)
    [ -f "$ENV_FILE" ] || die "missing $ENV_FILE (needs DB_PASSWORD for the snapshot)"
    # literal read — the password may start with '$', so do NOT shell-source it
    PW="$(grep -E '^[[:space:]]*DB_PASSWORD=' "$ENV_FILE" | head -1 | sed -E 's/^[[:space:]]*DB_PASSWORD=//' | tr -d '\r')"
    [ -n "$PW" ] || die "could not read DB_PASSWORD from $ENV_FILE"
    DB_URL="jdbc:postgresql://host.docker.internal:5432/saude-mandaguari"
    DB_USER="sysmar"; DB_PASS="$PW"
    DB_ARGS="--spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=none"
    log "DB = live non-prod snapshot ($DB_URL) — real data; writes persist to the shared snapshot."
    # reachability check
    (exec 3<>/dev/tcp/host.docker.internal/5432) 2>/dev/null || die "snapshot DB not reachable on host.docker.internal:5432"
    ;;
  local)
    command -v docker >/dev/null || die "docker not found (needed for 'local' mode)"
    export DB_USER=saude DB_PASSWORD=change_me_local_only   # also consumed by docker-compose.yml
    log "DB = local Docker postgres — starting container…"
    (cd "$ROOT" && docker compose up -d postgres)
    # wait for healthy
    for i in $(seq 1 30); do
      st="$(docker inspect -f '{{.State.Health.Status}}' receituario-postgres 2>/dev/null || echo none)"
      [ "$st" = "healthy" ] && break; sleep 2
    done
    DB_URL="jdbc:postgresql://localhost:5432/saude-mandaguari"
    DB_USER="saude"; DB_PASS="change_me_local_only"
    DB_ARGS=""   # Flyway ON + ddl validate (defaults) → builds schema in the fresh container
    log "DB = local Docker postgres ($DB_URL) — Flyway builds the schema (starts empty)."
    ;;
  *) die "unknown mode '$MODE' (use: snapshot | local)";;
esac

# --- build backend jar if missing ---
if [ ! -f "$JAR" ]; then
  log "backend jar missing — building (mvn -q -DskipTests package)…"
  (cd "$ROOT/backend" && mvn -q -DskipTests package) || die "backend build failed"
fi

# --- start backend on :8090 ---
log "starting backend on :$BE_PORT…"
( cd "$ROOT/backend" && nohup java -jar "$JAR" \
    --spring.profiles.active=local \
    --spring.datasource.url="$DB_URL" \
    --spring.datasource.username="$DB_USER" \
    --spring.datasource.password="$DB_PASS" \
    --server.port=$BE_PORT \
    --security.cert.enc-key="$CERT_ENC_KEY" \
    $DB_ARGS > "$STATE/backend.log" 2>&1 & echo $! > "$STATE/backend.pid" )

for i in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 4 -X POST "localhost:$BE_PORT/auth/login" \
           -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' 2>/dev/null || true)"
  [ "$code" = "200" ] && { log "backend UP (login 200)"; break; }
  kill -0 "$(cat "$STATE/backend.pid")" 2>/dev/null || die "backend died — see $STATE/backend.log"
  sleep 3
done
[ "${code:-}" = "200" ] || die "backend not ready on :$BE_PORT — see $STATE/backend.log"

# --- start frontend on :5173 ---
[ -d "$ROOT/frontend/node_modules" ] || { log "installing frontend deps…"; (cd "$ROOT/frontend" && npm install); }
log "starting frontend on :$FE_PORT…"
( cd "$ROOT/frontend" && nohup npm run dev -- --host --port $FE_PORT > "$STATE/frontend.log" 2>&1 & echo $! > "$STATE/frontend.pid" )

for i in $(seq 1 40); do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 4 "http://localhost:$FE_PORT/" 2>/dev/null || true)"
  [ "$code" = "200" ] && { log "frontend UP"; break; }
  sleep 2
done
[ "${code:-}" = "200" ] || die "frontend not ready on :$FE_PORT — see $STATE/frontend.log"

printf '\n\033[32m✓ Dev stack is up.\033[0m\n'
printf '  Frontend : http://localhost:%s/        (open this)\n' "$FE_PORT"
printf '  Backend  : http://localhost:%s/        (API; legacy app keeps :8080)\n' "$BE_PORT"
printf '  Login    : admin / admin123\n'
printf '  DB mode  : %s\n' "$MODE"
printf '  Logs     : %s\n' "$STATE/backend.log , $STATE/frontend.log"
printf '  Stop     : ./dev-down.sh\n'
