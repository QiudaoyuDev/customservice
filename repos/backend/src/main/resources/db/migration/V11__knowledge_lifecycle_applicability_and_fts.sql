CREATE TABLE knowledge_revision_applicability
(
    id                uuid PRIMARY KEY,
    revision_id       uuid        NOT NULL REFERENCES knowledge_revisions (id),
    product_model_id  uuid        NOT NULL REFERENCES product_models (id),
    product_variant_id uuid REFERENCES product_variants (id),
    region            varchar(16) NOT NULL,
    hardware_revision varchar(80),
    firmware_min      varchar(80),
    firmware_max      varchar(80),
    valid_from        timestamptz,
    valid_to          timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_applicability_scope ON knowledge_revision_applicability
    (product_model_id, product_variant_id, region, valid_from, valid_to);

ALTER TABLE knowledge_revisions
    ADD COLUMN index_status varchar(32) NOT NULL DEFAULT 'NOT_INDEXED',
    ADD COLUMN content_checksum varchar(64),
    ADD COLUMN parser_version varchar(80),
    ADD COLUMN failure_code varchar(80),
    ADD COLUMN failure_detail varchar(500);

ALTER TABLE knowledge_documents ADD COLUMN source_checksum varchar(64);
CREATE INDEX idx_knowledge_documents_tenant_checksum ON knowledge_documents (tenant_id, source_checksum);

CREATE TABLE knowledge_ocr_results
(
    id              uuid PRIMARY KEY,
    revision_id     uuid NOT NULL REFERENCES knowledge_revisions (id),
    raw_text        text NOT NULL,
    normalized_text text NOT NULL,
    confidence      numeric(5,4),
    language        varchar(16),
    page_from       integer,
    page_to         integer,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_knowledge_ocr_results_revision ON knowledge_ocr_results (revision_id);

CREATE INDEX idx_knowledge_revisions_publish_state ON knowledge_revisions (status, index_status, published_at, deprecated_at);

ALTER TABLE knowledge_chunks
    ADD COLUMN title_path varchar(1000),
    ADD COLUMN page_from integer,
    ADD COLUMN page_to integer,
    ADD COLUMN content_checksum varchar(64),
    ADD COLUMN token_count integer,
    ADD COLUMN metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(content, ''))) STORED;

CREATE INDEX idx_knowledge_chunks_search_vector ON knowledge_chunks USING gin (search_vector);

ALTER TABLE index_jobs
    ADD COLUMN lease_until timestamptz,
    ADD COLUMN heartbeat_at timestamptz,
    ADD COLUMN next_retry_at timestamptz,
    ADD COLUMN error_code varchar(80),
    ADD COLUMN max_attempts integer NOT NULL DEFAULT 3;

CREATE INDEX idx_index_jobs_claimable ON index_jobs (status, next_retry_at, lease_until, created_at);
