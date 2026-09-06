#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-$DEPLOY_DIR/.env}"
BACKUP_ROOT="${BACKUP_ROOT:-/srv/casehub/backups}"
FILE_STORAGE_PATH="${FILE_STORAGE_PATH:-}"
FILE_STORAGE_CONTAINER="${FILE_STORAGE_CONTAINER:-casehub-backend}"
DB_SERVICE="${DB_SERVICE:-postgres}"
QUIESCE_CMD="${BACKUP_QUIESCE_CMD:-}"
RESUME_CMD="${BACKUP_RESUME_CMD:-}"
BACKUP_KEEP_BACKEND_STOPPED="${BACKUP_KEEP_BACKEND_STOPPED:-false}"

fail() { echo "backup failed: $*" >&2; exit 1; }
source "$SCRIPT_DIR/common.sh"
compose() {
  if [[ -f "$COMPOSE_ENV_FILE" ]]; then
    docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
  else
    [[ -n "${POSTGRES_PASSWORD:-}" ]] || fail "POSTGRES_PASSWORD or COMPOSE_ENV_FILE is required"
    docker compose -f "$COMPOSE_FILE" "$@"
  fi
}
[[ "$BACKUP_ROOT" != "/" && -n "$BACKUP_ROOT" ]] || fail "unsafe backup destination"
if [[ -n "$FILE_STORAGE_PATH" ]]; then
  [[ "$FILE_STORAGE_PATH" != "/" && -d "$FILE_STORAGE_PATH" ]] || fail "FILE_STORAGE_PATH must be an existing non-root directory"
else
  docker inspect "$FILE_STORAGE_CONTAINER" >/dev/null 2>&1 || fail "FILE_STORAGE_CONTAINER does not exist"
fi
[[ "${BACKUP_QUIESCE_CONFIRMED:-false}" == "true" || -n "$QUIESCE_CMD" ]] \
  || fail "confirm the write-quiescence window with BACKUP_QUIESCE_CONFIRMED=true or BACKUP_QUIESCE_CMD"
[[ -z "$QUIESCE_CMD" || -n "$RESUME_CMD" ]] || fail "BACKUP_RESUME_CMD is required with BACKUP_QUIESCE_CMD"
resolve_database_identity

mkdir -p "$BACKUP_ROOT"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DEST="${1:-$BACKUP_ROOT/casehub-$STAMP}"
[[ "$DEST" == "$BACKUP_ROOT/"casehub-* ]] || fail "destination must be a casehub backup child"
[[ ! -e "$DEST" ]] || fail "destination already exists: $DEST"
mkdir -p "$DEST"

resumed=false
backup_complete=false
cleanup() {
  if [[ "$resumed" == false && -n "$RESUME_CMD" \
        && ("$BACKUP_KEEP_BACKEND_STOPPED" != true || "$backup_complete" != true) ]]; then
    sh -c "$RESUME_CMD" || echo "WARNING: resume command failed; writes may remain paused" >&2
    resumed=true
  fi
}
trap cleanup EXIT
if [[ "${BACKUP_MANAGE_COMPOSE:-false}" == "true" ]]; then
  compose stop backend
  if [[ -f "$COMPOSE_ENV_FILE" ]]; then
    RESUME_CMD="docker compose --env-file $(printf '%q' "$COMPOSE_ENV_FILE") -f $(printf '%q' "$COMPOSE_FILE") up -d backend"
  else
    RESUME_CMD="docker compose -f $(printf '%q' "$COMPOSE_FILE") up -d backend"
  fi
fi
if [[ -n "$QUIESCE_CMD" ]]; then
  sh -c "$QUIESCE_CMD"
fi

compose exec -T "$DB_SERVICE" \
  pg_dump --format=custom --no-owner --no-acl --dbname="$DB_NAME" --username="$DB_USER" \
  > "$DEST/database.dump"
if [[ -n "$FILE_STORAGE_PATH" ]]; then
  tar -C "$FILE_STORAGE_PATH" -cf "$DEST/file-storage.tar" .
else
  docker run --rm --volumes-from "$FILE_STORAGE_CONTAINER":ro -v "$DEST:/backup:rw" alpine:3.20 \
    tar -C /data/casehub -cf /backup/file-storage.tar .
fi
chmod 600 "$DEST/database.dump" "$DEST/file-storage.tar"
[[ -s "$DEST/database.dump" ]] || fail "database dump is empty"
[[ -s "$DEST/file-storage.tar" ]] || fail "file-storage archive is empty"

sha256() {
  if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'; else sha256sum "$1" | awk '{print $1}'; fi
}
DB_HASH="$(sha256 "$DEST/database.dump")"
FILES_HASH="$(sha256 "$DEST/file-storage.tar")"
{
  printf 'format=casehub-backup-v1\n'
  printf 'created_at=%s\n' "$STAMP"
  printf 'database=database.dump\n'
  printf 'database_sha256=%s\n' "$DB_HASH"
  printf 'file_storage=file-storage.tar\n'
  printf 'file_storage_sha256=%s\n' "$FILES_HASH"
  printf 'database_name=%s\n' "$DB_NAME"
  printf 'database_user=%s\n' "$DB_USER"
} > "$DEST/manifest.txt"
sync
backup_complete=true
echo "backup created: $DEST"
