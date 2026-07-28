#!/usr/bin/env bash
set -Eeuo pipefail

INFRA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${INFRA_ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}." >&2
  exit 1
fi

# .env values are generated without shell metacharacters by bootstrap.sh.
set -a
source "${ENV_FILE}"
set +a

check_http() {
  local name="$1"
  local url="$2"
  shift 2
  if curl --fail --silent --show-error --max-time 8 "$@" "${url}" >/dev/null; then
    echo "[OK] ${name}"
  else
    echo "[FAIL] ${name}" >&2
    return 1
  fi
}

cd "${INFRA_ROOT}"
docker compose --env-file .env -f compose.yaml exec -T postgres \
  pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null
echo "[OK] PostgreSQL"

check_http "MinIO" "http://${INFRA_BIND_ADDRESS}:${MINIO_API_PORT}/minio/health/live"
check_http "Qdrant" "http://${INFRA_BIND_ADDRESS}:${QDRANT_HTTP_PORT}/healthz" -H "api-key: ${QDRANT_API_KEY}"
check_http "OCR" "http://${INFRA_BIND_ADDRESS}:${OCR_PORT}/health"
check_http "Embedding" "http://${INFRA_BIND_ADDRESS}:${EMBEDDING_PORT}/health"
