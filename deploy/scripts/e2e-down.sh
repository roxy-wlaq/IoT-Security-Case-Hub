#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

docker compose --project-name "${E2E_COMPOSE_PROJECT:-casehub-e2e}" \
  -f deploy/docker-compose.yml -f deploy/docker-compose.e2e.yml \
  down --volumes --remove-orphans
