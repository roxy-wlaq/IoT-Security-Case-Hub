#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_YML="$ROOT_DIR/backend/src/main/resources/application.yml"
PROD_YML="$ROOT_DIR/backend/src/main/resources/application-prod.yml"
ENV_EXAMPLE="$ROOT_DIR/deploy/.env.example"

grep -Fq 'username: ${DB_USER}' "$APP_YML"
grep -Fq 'password: ${DB_PASSWORD}' "$APP_YML"
grep -Fq 'include: health' "$PROD_YML"
! grep -Eq 'include:.*(env|beans|configprops|heapdump|mappings)' "$PROD_YML"
! rg -n -i --glob '!*.sh' 'BEGIN (RSA |EC )?PRIVATE KEY' "$ROOT_DIR/deploy"
! grep -Eiq 'password[=:][[:space:]]*(admin|secret|password|changeme)([[:space:]]|$)' "$ENV_EXAMPLE"

if [[ -d "$ROOT_DIR/frontend/dist" ]]; then
  ! rg -n -i --glob '!*.map' 'BEGIN (RSA |EC )?PRIVATE KEY|DB_PASSWORD' "$ROOT_DIR/frontend/dist"
fi

echo 'Production secret and actuator static contract: PASS'
