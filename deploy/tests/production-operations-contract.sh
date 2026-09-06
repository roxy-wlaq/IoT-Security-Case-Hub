#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURE_DIR="$SCRIPT_DIR/fixtures"

fail() { echo "production operations contract failed: $*" >&2; exit 1; }
assert_contains() { grep -Fq -- "$2" "$1" || fail "expected '$2' in $1"; }

tmp_root="$(mktemp -d "${TMPDIR:-/tmp}/casehub-ops-contract.XXXXXX")"
env_file="$tmp_root/deploy.env"
storage="$tmp_root/storage"
log_file="$tmp_root/docker.log"
backup_root="$tmp_root/backups"
mkdir -p "$storage" "$backup_root"
printf 'POSTGRES_DB=casehub_prod\nPOSTGRES_USER=casehub_app\nPOSTGRES_PASSWORD=fixture-only\n' > "$env_file"
printf 'representative evidence bytes\n' > "$storage/evidence.bin"

export PATH="$FIXTURE_DIR:$PATH"
export FAKE_DOCKER_LOG="$log_file"
export FAKE_DOCKER_ENV_FILE="$env_file"
export COMPOSE_ENV_FILE="$env_file"
export COMPOSE_FILE="$REPO_ROOT/deploy/docker-compose.yml"
export FILE_STORAGE_PATH="$storage"
export BACKUP_ROOT="$backup_root"
export BACKUP_QUIESCE_CONFIRMED=true
export CASEHUB_BASE_URL=https://fixture.invalid
export HEALTH_CHECK_SCRIPT="$FIXTURE_DIR/health-check-pass.sh"

upgrade_output="$(UPGRADE_CONFIRM=YES "$REPO_ROOT/deploy/scripts/upgrade.sh")"
backup_dir="${upgrade_output##*: }"
[[ -d "$backup_dir" ]] || fail "upgrade did not report a backup directory"
assert_contains "$backup_dir/manifest.txt" "database_name=casehub_prod"
assert_contains "$backup_dir/manifest.txt" "database_user=casehub_app"

stop_line="$(grep -n 'compose .*stop backend' "$log_file" | head -n1 | cut -d: -f1)"
release_line="$(grep -n 'compose .*up -d --build' "$log_file" | tail -n1 | cut -d: -f1)"
[[ -n "$stop_line" && -n "$release_line" && "$stop_line" -lt "$release_line" ]] \
  || fail "upgrade did not stop backend before starting the new release"
if sed -n "$((stop_line + 1)),$((release_line - 1))p" "$log_file" | grep -Fq 'up -d backend'; then
  fail "old backend was resumed between backup and new release"
fi

restore_target="$tmp_root/restore-storage"
mkdir -p "$restore_target"
export FILE_STORAGE_PATH="$restore_target"
export PREVIOUS_BACKEND_IMAGE=casehub-backend:1.0.0
export PREVIOUS_NGINX_IMAGE=casehub-nginx:1.0.0
export CASEHUB_BACKEND_IMAGE=casehub-backend:2.0.0
export CASEHUB_NGINX_IMAGE=casehub-nginx:2.0.0
export ROLLBACK_CONFIRM=YES
export RESTORE_BACKUP_DIR="$backup_dir"

rollback_output="$($REPO_ROOT/deploy/scripts/rollback.sh)"
[[ "$rollback_output" == *"rollback completed"* ]] || fail "rollback did not complete"
final_up_line="$(grep -n 'compose .*up -d --no-build' "$log_file" | tail -n1 | cut -d: -f1)"
[[ -n "$final_up_line" ]] || fail "rollback did not start a release"
assert_contains "$log_file" "CASEHUB_BACKEND_IMAGE=casehub-backend:1.0.0"
assert_contains "$log_file" "CASEHUB_NGINX_IMAGE=casehub-nginx:1.0.0"
rollback_stop_line="$(grep -n 'CASEHUB_BACKEND_IMAGE=casehub-backend:2.0.0.*compose .*stop backend nginx' "$log_file" | tail -n1 | cut -d: -f1)"
[[ -n "$rollback_stop_line" && "$rollback_stop_line" -lt "$final_up_line" ]] \
  || fail "rollback did not stop the current release before selecting the previous one"
if sed -n "$((rollback_stop_line + 1)),$((final_up_line - 1))p" "$log_file" | grep -Eq 'compose .*up'; then
  fail "a backend was started before the previous release was selected"
fi

echo "Production operations contract: PASS"
