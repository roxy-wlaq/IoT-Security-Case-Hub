#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-$DEPLOY_DIR/.env}"
PREVIOUS_BACKEND_IMAGE="${PREVIOUS_BACKEND_IMAGE:-}"
PREVIOUS_NGINX_IMAGE="${PREVIOUS_NGINX_IMAGE:-}"

[[ "${ROLLBACK_CONFIRM:-}" == "YES" ]] || { echo 'set ROLLBACK_CONFIRM=YES to run rollback' >&2; exit 1; }
[[ -n "$PREVIOUS_BACKEND_IMAGE" && -n "$PREVIOUS_NGINX_IMAGE" ]] \
  || { echo 'PREVIOUS_BACKEND_IMAGE and PREVIOUS_NGINX_IMAGE are required' >&2; exit 1; }
[[ "$PREVIOUS_BACKEND_IMAGE" != *[[:space:]]* && "$PREVIOUS_NGINX_IMAGE" != *[[:space:]]* ]] \
  || { echo 'previous image references must not contain whitespace' >&2; exit 1; }

compose() {
  if [[ -f "$COMPOSE_ENV_FILE" ]]; then
    docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
  else
    docker compose -f "$COMPOSE_FILE" "$@"
  fi
}

compose stop backend nginx
if [[ -n "${RESTORE_BACKUP_DIR:-}" ]]; then
  COMPOSE_FILE="$COMPOSE_FILE" COMPOSE_ENV_FILE="$COMPOSE_ENV_FILE" RESTORE_CONFIRM=YES \
    RESTORE_START_BACKEND=false RESTORE_VERIFY_HEALTH=false \
    "$SCRIPT_DIR/restore.sh" "$RESTORE_BACKUP_DIR"
fi
CASEHUB_BACKEND_IMAGE="$PREVIOUS_BACKEND_IMAGE" CASEHUB_NGINX_IMAGE="$PREVIOUS_NGINX_IMAGE" \
  compose up -d --no-build
HEALTH_CHECK_SCRIPT="${HEALTH_CHECK_SCRIPT:-$SCRIPT_DIR/health-check.sh}"
COMPOSE_FILE="$COMPOSE_FILE" CASEHUB_BASE_URL="${CASEHUB_BASE_URL:-https://localhost}" \
  CASEHUB_INSECURE_TLS="${CASEHUB_INSECURE_TLS:-false}" "$HEALTH_CHECK_SCRIPT"
echo 'rollback completed'
