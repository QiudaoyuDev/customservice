#!/usr/bin/env bash
set -Eeuo pipefail

INFRA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${INFRA_ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
target="/backups/${POSTGRES_DB}-${timestamp}.dump"

cd "${INFRA_ROOT}"
docker compose --env-file .env -f compose.yaml exec -T postgres sh -c \
  "PGPASSWORD='${POSTGRES_PASSWORD}' pg_dump -U '${POSTGRES_USER}' -d '${POSTGRES_DB}' -Fc -f '${target}'"

echo "PostgreSQL backup created: ${INFRA_ROOT}/backups/postgres/${POSTGRES_DB}-${timestamp}.dump"
