CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- pgvector is intentionally not enabled here. The MVP starts with Qdrant for
-- vector retrieval; enable pgvector later only when its image/extension is
-- explicitly selected and tested for the target PostgreSQL version.
