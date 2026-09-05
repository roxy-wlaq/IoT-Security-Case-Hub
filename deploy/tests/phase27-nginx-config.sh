#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONF="$ROOT_DIR/deploy/nginx/conf.d/casehub.conf"
COMPOSE="$ROOT_DIR/deploy/docker-compose.yml"
DOCKERFILE="$ROOT_DIR/deploy/nginx.Dockerfile"

require_text() {
  local file="$1"
  local text="$2"
  if ! grep -Fq -- "$text" "$file"; then
    echo "missing '$text' in $file" >&2
    exit 1
  fi
}

require_text "$CONF" 'listen 443 ssl'
require_text "$CONF" 'ssl_protocols TLSv1.2 TLSv1.3'
require_text "$CONF" 'return 308 https://$host$request_uri'
require_text "$CONF" 'Strict-Transport-Security'
require_text "$CONF" 'Content-Security-Policy'
require_text "$CONF" 'X-Content-Type-Options'
require_text "$CONF" 'X-Frame-Options'
require_text "$CONF" 'Referrer-Policy'
require_text "$CONF" 'location = /actuator/health'
require_text "$CONF" 'location ^~ /actuator/'
require_text "$CONF" 'limit_req zone=login_limit burst=5 nodelay'
require_text "$CONF" 'proxy_set_header X-Forwarded-Proto $scheme'
require_text "$COMPOSE" '"${HTTPS_PORT:-443}:443"'
require_text "$COMPOSE" ':/etc/nginx/certs:ro'
require_text "$DOCKERFILE" 'EXPOSE 80 443'

if grep -Fq 'Access-Control-Allow-Origin "*"' "$CONF"; then
  echo 'wildcard credentialed CORS must not be configured' >&2
  exit 1
fi

echo 'Phase 27 Nginx static contract: PASS'
