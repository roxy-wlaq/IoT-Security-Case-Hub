#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_ROOT="${BACKUP_ROOT:-/srv/casehub/backups}"

[[ "${UPGRADE_CONFIRM:-}" == "YES" ]] || { echo 'set UPGRADE_CONFIRM=YES to run an upgrade' >&2; exit 1; }
BACKUP_DIR="$(COMPOSE_ENV_FILE="$COMPOSE_ENV_FILE" COMPOSE_FILE="$COMPOSE_FILE" BACKUP_MANAGE_COMPOSE=true BACKUP_QUIESCE_CONFIRMED=true \
  BACKUP_KEEP_BACKEND_STOPPED=true \
  "$SCRIPT_DIR/backup.sh" "$BACKUP_ROOT/casehub-upgrade-$(date -u +%Y%m%dT%H%M%SZ)")"
BACKUP_DIR="${BACKUP_DIR##*: }"
if [[ -f "$COMPOSE_ENV_FILE" ]]; then
  docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" up -d --build
else
  docker compose -f "$COMPOSE_FILE" up -d --build
fi
HEALTH_CHECK_SCRIPT="${HEALTH_CHECK_SCRIPT:-$SCRIPT_DIR/health-check.sh}"
COMPOSE_FILE="$COMPOSE_FILE" CASEHUB_BASE_URL="${CASEHUB_BASE_URL:-https://localhost}" \
  CASEHUB_INSECURE_TLS="${CASEHUB_INSECURE_TLS:-false}" "$HEALTH_CHECK_SCRIPT"
echo "upgrade completed; pre-upgrade backup: $BACKUP_DIR"
