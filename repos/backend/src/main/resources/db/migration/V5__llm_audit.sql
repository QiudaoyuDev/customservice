CREATE TABLE model_configurations
(
    id                uuid PRIMARY KEY,
    tenant_id         uuid         NOT NULL REFERENCES tenants (id),
    name              varchar(120) NOT NULL,
    base_url          varchar(500) NOT NULL,
    model_name        varchar(160) NOT NULL,
    encrypted_api_key text         NOT NULL,
    enabled           boolean      NOT NULL DEFAULT true,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);
CREATE TABLE answer_traces
(
    id                     uuid PRIMARY KEY,
    conversation_id        uuid        NOT NULL REFERENCES conversations (id),
    message_id             uuid REFERENCES messages (id),
    model_configuration_id uuid REFERENCES model_configurations (id),
    intent                 varchar(40) NOT NULL,
    outcome                varchar(40) NOT NULL,
    latency_ms             bigint,
    token_count            integer,
    created_at             timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE answer_citations
(
    id              uuid PRIMARY KEY,
    answer_trace_id uuid        NOT NULL REFERENCES answer_traces (id),
    revision_id     uuid        NOT NULL REFERENCES knowledge_revisions (id),
    chunk_id        uuid        NOT NULL REFERENCES knowledge_chunks (id),
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_answer_traces_conversation ON answer_traces (conversation_id, created_at);
