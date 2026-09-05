#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-$DEPLOY_DIR/.env}"

[[ "${ROLLBACK_CONFIRM:-}" == "YES" ]] || { echo 'set ROLLBACK_CONFIRM=YES to run rollback' >&2; exit 1; }
if [[ -n "${RESTORE_BACKUP_DIR:-}" ]]; then
  COMPOSE_FILE="$COMPOSE_FILE" COMPOSE_ENV_FILE="$COMPOSE_ENV_FILE" RESTORE_CONFIRM=YES RESTORE_VERIFY_HEALTH=false \
    "$SCRIPT_DIR/restore.sh" "$RESTORE_BACKUP_DIR"
else
  echo 'No database restore requested; this path is only valid when the old image is schema-compatible.'
  if [[ -f "$COMPOSE_ENV_FILE" ]]; then
    docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" up -d
  else
    docker compose -f "$COMPOSE_FILE" up -d
  fi
fi
COMPOSE_FILE="$COMPOSE_FILE" CASEHUB_BASE_URL="${CASEHUB_BASE_URL:-https://localhost}" \
  CASEHUB_INSECURE_TLS="${CASEHUB_INSECURE_TLS:-false}" "$SCRIPT_DIR/health-check.sh"
echo 'rollback completed'
