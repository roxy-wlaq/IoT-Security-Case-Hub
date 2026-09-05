#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-$DEPLOY_DIR/.env}"
FILE_STORAGE_PATH="${FILE_STORAGE_PATH:-}"
FILE_STORAGE_CONTAINER="${FILE_STORAGE_CONTAINER:-casehub-backend}"
DB_SERVICE="${DB_SERVICE:-postgres}"
DB_NAME="${POSTGRES_DB:-${DB_NAME:-casehub}}"
DB_USER="${POSTGRES_USER:-${DB_USER:-casehub}}"
BACKUP_DIR="${1:-}"

fail() { echo "restore failed: $*" >&2; exit 1; }
compose() {
  if [[ -f "$COMPOSE_ENV_FILE" ]]; then
    docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
  else
    [[ -n "${POSTGRES_PASSWORD:-}" ]] || fail "POSTGRES_PASSWORD or COMPOSE_ENV_FILE is required"
    docker compose -f "$COMPOSE_FILE" "$@"
  fi
}
[[ "${RESTORE_CONFIRM:-}" == "YES" ]] || fail "set RESTORE_CONFIRM=YES for destructive restore"
[[ -n "$BACKUP_DIR" && -d "$BACKUP_DIR" ]] || fail "backup directory is required"
BACKUP_ROOT="${BACKUP_ROOT:-$(dirname "$BACKUP_DIR")}" 
[[ "$BACKUP_DIR" == "$BACKUP_ROOT"/casehub-* ]] || fail "backup must be a casehub backup child"
[[ -f "$BACKUP_DIR/manifest.txt" && -f "$BACKUP_DIR/database.dump" && -f "$BACKUP_DIR/file-storage.tar" ]] \
  || fail "backup set is incomplete"
if [[ -n "$FILE_STORAGE_PATH" ]]; then
  [[ "$FILE_STORAGE_PATH" != "/" ]] || fail "unsafe file storage target"
else
  docker inspect "$FILE_STORAGE_CONTAINER" >/dev/null 2>&1 || fail "FILE_STORAGE_CONTAINER does not exist"
fi

sha256() {
  if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | awk '{print $1}'; else sha256sum "$1" | awk '{print $1}'; fi
}
expected_db="$(sed -n 's/^database_sha256=//p' "$BACKUP_DIR/manifest.txt")"
expected_files="$(sed -n 's/^file_storage_sha256=//p' "$BACKUP_DIR/manifest.txt")"
[[ -n "$expected_db" && "$expected_db" == "$(sha256 "$BACKUP_DIR/database.dump")" ]] || fail "database checksum mismatch"
[[ -n "$expected_files" && "$expected_files" == "$(sha256 "$BACKUP_DIR/file-storage.tar")" ]] || fail "file storage checksum mismatch"
[[ "$(sed -n 's/^format=//p' "$BACKUP_DIR/manifest.txt")" == casehub-backup-v1 ]] || fail "unsupported backup format"

STAGING="$(mktemp -d "${TMPDIR:-/tmp}/casehub-restore.XXXXXX")"
cleanup() { rm -rf "$STAGING"; }
trap cleanup EXIT
compose stop backend

compose exec -T "$DB_SERVICE" \
  dropdb --if-exists --username="$DB_USER" "$DB_NAME"
compose exec -T "$DB_SERVICE" \
  createdb --username="$DB_USER" "$DB_NAME"
cat "$BACKUP_DIR/database.dump" | compose exec -T "$DB_SERVICE" \
  pg_restore --no-owner --no-acl --dbname="$DB_NAME" --username="$DB_USER"

if [[ -n "$FILE_STORAGE_PATH" ]]; then
  mkdir -p "$FILE_STORAGE_PATH"
  mv "$FILE_STORAGE_PATH" "$STAGING/previous-files"
  mkdir -p "$FILE_STORAGE_PATH"
  tar -C "$FILE_STORAGE_PATH" -xf "$BACKUP_DIR/file-storage.tar"
else
  docker run --rm --volumes-from "$FILE_STORAGE_CONTAINER" -i alpine:3.20 \
    sh -c 'find /data/casehub -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +; tar -C /data/casehub -xf -' \
    < "$BACKUP_DIR/file-storage.tar"
fi
compose up -d backend
if [[ "${RESTORE_VERIFY_HEALTH:-true}" == "true" ]]; then
  CASEHUB_BASE_URL="${CASEHUB_BASE_URL:-https://localhost}" \
  CASEHUB_INSECURE_TLS="${CASEHUB_INSECURE_TLS:-false}" "$SCRIPT_DIR/health-check.sh"
fi
if [[ -d "$STAGING/previous-files" ]]; then rm -rf "$STAGING/previous-files"; fi
echo "restore completed from: $BACKUP_DIR"
