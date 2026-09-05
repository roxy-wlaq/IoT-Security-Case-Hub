#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CASEHUB_BASE_URL:-https://localhost}"
CURL_ARGS=(--fail --silent --show-error --max-time "${CASEHUB_HEALTH_TIMEOUT_SECONDS:-10}")
if [[ "${CASEHUB_INSECURE_TLS:-false}" == "true" ]]; then
  CURL_ARGS+=(--insecure)
fi

fetch_with_retry() {
  local url="$1"
  local attempt
  local response
  for ((attempt = 1; attempt <= ${CASEHUB_HEALTH_RETRIES:-30}; attempt++)); do
    if response="$(curl "${CURL_ARGS[@]}" "$url" 2>/dev/null)"; then
      printf '%s' "$response"
      return 0
    fi
    sleep "${CASEHUB_HEALTH_RETRY_SECONDS:-2}"
  done
  echo "health endpoint unavailable: $url" >&2
  return 1
}

healthz="$(fetch_with_retry "$BASE_URL/healthz")"
if [[ "$healthz" != *"ok"* ]]; then
  echo "Nginx health endpoint returned an unexpected body" >&2
  exit 1
fi

backend="$(fetch_with_retry "$BASE_URL/actuator/health")"
if [[ "$backend" != *'"status":"UP"'* && "$backend" != *'"status": "UP"'* ]]; then
  echo "Backend health is not UP: $backend" >&2
  exit 1
fi

echo "healthy: $BASE_URL"
