#!/usr/bin/env bash

# Resolve database identity from the effective PostgreSQL container. This avoids
# assuming the caller's shell exported the values from the Compose env file.
resolve_database_identity() {
  local container_id env_text
  container_id="$(compose ps -aq "$DB_SERVICE" | awk 'NF { print $1; exit }')"
  [[ -n "$container_id" ]] || fail "PostgreSQL container does not exist"

  env_text="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$container_id")"
  DB_NAME="$(printf '%s\n' "$env_text" | awk -F= '$1 == "POSTGRES_DB" { print substr($0, index($0, "=") + 1); exit }')"
  DB_USER="$(printf '%s\n' "$env_text" | awk -F= '$1 == "POSTGRES_USER" { print substr($0, index($0, "=") + 1); exit }')"
  [[ -n "$DB_NAME" && -n "$DB_USER" ]] || fail "effective PostgreSQL DB/user identity is unavailable"
}

manifest_value() {
  sed -n "s/^$1=//p" "$2"
}
