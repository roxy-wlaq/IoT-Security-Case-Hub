#!/usr/bin/env bash

validate_e2e_project_name() {
  local project_name="${1:-}"

  if [[ ! "$project_name" =~ ^casehub-e2e(-[a-z0-9][a-z0-9_-]*)?$ ]]; then
    printf 'Refusing unsafe E2E Compose project name: %q\n' "$project_name" >&2
    printf 'Allowed names match casehub-e2e or casehub-e2e-* (lowercase).\n' >&2
    return 1
  fi
}
