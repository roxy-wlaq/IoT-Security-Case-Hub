#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/deploy/scripts/e2e-project-guard.sh"

E2E_PROJECT="${E2E_COMPOSE_PROJECT:-casehub-e2e}"
validate_e2e_project_name "$E2E_PROJECT"

docker compose --project-name "$E2E_PROJECT" \
  -f deploy/docker-compose.yml -f deploy/docker-compose.e2e.yml \
  down --volumes --remove-orphans
