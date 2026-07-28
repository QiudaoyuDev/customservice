CREATE TABLE handoff_requests
(
    id                 uuid PRIMARY KEY,
    tenant_id          uuid         NOT NULL REFERENCES tenants (id),
    conversation_id    uuid         NOT NULL REFERENCES conversations (id),
    idempotency_key    varchar(160) NOT NULL,
    status             varchar(32)  NOT NULL,
    reason             varchar(300) NOT NULL,
    summary            text         NOT NULL,
    contact_authorized boolean      NOT NULL DEFAULT false,
    assigned_to        uuid REFERENCES users (id),
    resolution         varchar(32),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    closed_at          timestamptz,
    UNIQUE (tenant_id, idempotency_key)
);
CREATE TABLE handoff_notification_jobs
(
    id                 uuid PRIMARY KEY,
    handoff_request_id uuid        NOT NULL REFERENCES handoff_requests (id),
    status             varchar(32) NOT NULL,
    attempts           integer     NOT NULL DEFAULT 0,
    error_message      text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    completed_at       timestamptz
);
