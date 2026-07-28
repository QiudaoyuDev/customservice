#!/usr/bin/env bash
set -Eeuo pipefail

INFRA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${INFRA_ROOT}/.env"
EXAMPLE_FILE="${INFRA_ROOT}/.env.example"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI not found. Run sudo bash scripts/install-docker-centos.sh first." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose plugin not found. Install docker-compose-plugin first." >&2
  exit 1
fi

if [[ -e "${ENV_FILE}" && "${1:-}" != "--force" ]]; then
  echo "${ENV_FILE} already exists. Use --force only when intentionally regenerating secrets." >&2
  exit 1
fi

new_secret() {
  openssl rand -hex "${1:-32}"
}

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to generate secrets. Install it with dnf install -y openssl." >&2
  exit 1
fi

umask 077
sed \
  -e "s/CHANGE_ME_POSTGRES_PASSWORD/$(new_secret 32)/" \
  -e "s/CHANGE_ME_MINIO_PASSWORD/$(new_secret 40)/" \
  -e "s/CHANGE_ME_QDRANT_API_KEY/$(new_secret 32)/" \
  "${EXAMPLE_FILE}" > "${ENV_FILE}"

mkdir -p "${INFRA_ROOT}/backups/postgres"
chmod 700 "${INFRA_ROOT}/backups"

echo "Created ${ENV_FILE} and ${INFRA_ROOT}/backups/postgres"
echo "Review ports and local model names before first start. Keep .env outside version control."
