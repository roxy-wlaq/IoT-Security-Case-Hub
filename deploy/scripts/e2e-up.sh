#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

E2E_PROJECT="${E2E_COMPOSE_PROJECT:-casehub-e2e}"
E2E_CERT_DIR="${E2E_TLS_CERT_DIR:-${TMPDIR:-/tmp}/casehub-e2e-certs}"
E2E_HTTP_PORT="${E2E_HTTP_PORT:-8081}"
E2E_HTTPS_PORT="${E2E_HTTPS_PORT:-8443}"

mkdir -p "$E2E_CERT_DIR"
chmod 700 "$E2E_CERT_DIR"
if [[ ! -f "$E2E_CERT_DIR/fullchain.pem" || ! -f "$E2E_CERT_DIR/privkey.pem" ]]; then
  openssl req -x509 -nodes -newkey rsa:2048 -days 2 \
    -keyout "$E2E_CERT_DIR/privkey.pem" \
    -out "$E2E_CERT_DIR/fullchain.pem" \
    -subj '/CN=127.0.0.1' \
    -addext 'subjectAltName=IP:127.0.0.1,DNS:localhost'
  chmod 600 "$E2E_CERT_DIR/privkey.pem"
fi

export E2E_COMPOSE_PROJECT="$E2E_PROJECT"
export E2E_TLS_CERT_DIR="$E2E_CERT_DIR"
export E2E_HTTP_PORT E2E_HTTPS_PORT
export POSTGRES_DB="${POSTGRES_DB:-casehub}"
export POSTGRES_USER="${POSTGRES_USER:-casehub}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-E2ePostgres!2026}"
export DB_NAME="${DB_NAME:-$POSTGRES_DB}"
export DB_USER="${DB_USER:-$POSTGRES_USER}"
export DB_PASSWORD="${DB_PASSWORD:-$POSTGRES_PASSWORD}"
export CASEHUB_BOOTSTRAP_ADMIN_USERNAME="${CASEHUB_BOOTSTRAP_ADMIN_USERNAME:-e2e-admin}"
export CASEHUB_BOOTSTRAP_ADMIN_DISPLAY_NAME="${CASEHUB_BOOTSTRAP_ADMIN_DISPLAY_NAME:-E2E Admin}"
export CASEHUB_BOOTSTRAP_ADMIN_PASSWORD="${CASEHUB_BOOTSTRAP_ADMIN_PASSWORD:-E2ePassword!2026}"

docker compose --project-name "$E2E_PROJECT" \
  -f deploy/docker-compose.yml -f deploy/docker-compose.e2e.yml \
  up --build -d

printf 'E2E_BASE_URL=https://127.0.0.1:%s\nE2E_COMPOSE_PROJECT=%s\nE2E_TLS_CERT_DIR=%s\n' \
  "$E2E_HTTPS_PORT" "$E2E_PROJECT" "$E2E_CERT_DIR"
