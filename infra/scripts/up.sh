#!/usr/bin/env bash
set -Eeuo pipefail

INFRA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${INFRA_ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Run bash scripts/bootstrap.sh first." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose plugin not available." >&2
  exit 1
fi

cd "${INFRA_ROOT}"
docker compose --env-file .env -f compose.yaml up -d --build "$@"
