#!/bin/bash
# smoke-test.sh
# A post-deployment smoke test that verifies the core services are responding.
# Jenkins calls this after 'docker compose up -d'.
# Exits with code 1 on any failure, which marks the Jenkins stage as failed.

set -euo pipefail

# Configuration

BACKEND_URL="http://localhost:80/actuator/health"
FRONTEND_URL="http://localhost:80"
MAX_WAIT_SECONDS=60
POLL_INTERVAL=5

# Helpers

log()  { echo "[smoke-test] $*"; }
fail() { echo "[smoke-test] FAIL: $*" >&2; exit 1; }

wait_for_url() {
    local url="$1"
    local label="$2"
    local elapsed=0

    log "Waiting for ${label} at ${url} (timeout: ${MAX_WAIT_SECONDS}s)..."

    while true; do
        if curl -sf --max-time 5 "${url}" -o /dev/null; then
            log "${label} is UP."
            return 0
        fi

        elapsed=$((elapsed + POLL_INTERVAL))
        if [ "${elapsed}" -ge "${MAX_WAIT_SECONDS}" ]; then
            fail "${label} did not become healthy within ${MAX_WAIT_SECONDS}s."
        fi

        log "  ${label} not ready yet. Retrying in ${POLL_INTERVAL}s... (${elapsed}/${MAX_WAIT_SECONDS}s elapsed)"
        sleep "${POLL_INTERVAL}"
    done
}

# Tests

log "Starting smoke tests..."

# Backend health endpoint
wait_for_url "${BACKEND_URL}" "Backend (Actuator /health)"

# Verify the health response body reports UP
HEALTH_STATUS=$(curl -sf --max-time 5 "${BACKEND_URL}" | grep -o '"status":"UP"' || true)
if [ -z "${HEALTH_STATUS}" ]; then
    fail "Backend /actuator/health responded but status is not UP. Check application logs."
fi
log "Backend health status: UP"

# Frontend reachability (Nginx serving the React app)
wait_for_url "${FRONTEND_URL}" "Frontend (Nginx)"

log "All smoke tests passed."
