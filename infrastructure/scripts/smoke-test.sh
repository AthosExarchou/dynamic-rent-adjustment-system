#!/bin/bash
# smoke-test.sh
# A post-deployment smoke test that verifies the core services are responding.
# Jenkins calls this after 'docker compose up -d'.
# Exits with code 1 on any failure, which marks the Jenkins stage as failed.

set -euo pipefail

# Configuration

BACKEND_URL="http://localhost:80/api/actuator/health"
FRONTEND_URL="http://localhost:80"
MAX_WAIT_SECONDS=180
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

# Scraper -> backend authentication
# The backend fails closed, so an unset key means every scraper push gets 401
if [ -n "${SCRAPER_API_KEY:-}" ]; then
    IMPORT_URL="http://localhost:80/api/external-import/listings"

    STATUS=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
        -X POST "${IMPORT_URL}" \
        -H 'Content-Type: application/json' \
        -H "X-Scraper-Key: ${SCRAPER_API_KEY}" \
        -d '[]')

    if [ "${STATUS}" != "400" ]; then
        fail "Scraper API key was rejected (expected 400 for an empty payload, got ${STATUS}). Check that SCRAPER_API_KEY reached the backend container."
    fi
    log "Scraper import endpoint accepts the configured API key."

    STATUS=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
        -X POST "${IMPORT_URL}" \
        -H 'Content-Type: application/json' \
        -H "X-Scraper-Key: definitely-not-the-configured-key" \
        -d '[]')

    if [ "${STATUS}" != "401" ]; then
        fail "Scraper import endpoint accepted an invalid API key (expected 401, got ${STATUS})."
    fi
    log "Scraper import endpoint rejects invalid API keys."
else
    log "SCRAPER_API_KEY is not set; skipping scraper import authentication checks."
fi

log "All smoke tests passed."
