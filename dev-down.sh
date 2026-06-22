#!/usr/bin/env bash
# Stops the dev stack started by dev-up.sh (backend :8090 + frontend :5173).
# Leaves the local Docker postgres running by default; pass --db to also stop it.
# Does NOT touch the legacy app on :8080.
#
# Usage: ./dev-down.sh [--db] [--quiet]
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE="$ROOT/.devstack"
QUIET=0; STOP_DB=0
for a in "$@"; do case "$a" in --quiet) QUIET=1;; --db) STOP_DB=1;; esac; done
say() { [ "$QUIET" = 1 ] || printf '\033[36m[dev-down]\033[0m %s\n' "$*"; }

kill_pidfile() { # $1=pidfile $2=label
  local f="$1" l="$2" pid
  [ -f "$f" ] || return 0
  pid="$(cat "$f" 2>/dev/null)"
  if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
    # kill the whole process group (npm spawns vite as a child)
    kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null
    say "stopped $l (pid $pid)"
  fi
  rm -f "$f"
}

kill_pidfile "$STATE/frontend.pid" "frontend"
kill_pidfile "$STATE/backend.pid"  "backend"

# Fallbacks (in case pidfiles are stale). The char-class avoids this command matching its own
# shell process under pkill -f (that self-match was the source of odd exits).
pkill -f 'receituari[o]-backend.jar' 2>/dev/null && say "stopped stray backend" || true
# stop any vite dev server under this repo's frontend
pkill -f "[v]ite.*$ROOT/frontend" 2>/dev/null || pkill -f '[v]ite' 2>/dev/null && say "stopped stray vite" || true

if [ "$STOP_DB" = 1 ]; then
  say "stopping local Docker postgres…"
  (cd "$ROOT" && docker compose down) 2>/dev/null || true
fi

say "done."
