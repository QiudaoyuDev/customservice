CREATE TABLE troubleshoot_flow_version_snapshots
(
    id              uuid PRIMARY KEY,
    flow_id         uuid NOT NULL REFERENCES troubleshoot_flows(id),
    version_no      integer NOT NULL,
    status          varchar(32) NOT NULL,
    definition      jsonb NOT NULL,
    published_at    timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (flow_id, version_no)
);

CREATE TABLE conversation_flow_sessions
(
    id              uuid PRIMARY KEY,
    conversation_id uuid NOT NULL REFERENCES conversations(id),
    flow_version_id uuid NOT NULL REFERENCES troubleshoot_flow_version_snapshots(id),
    current_node_key varchar(100),
    failure_count   integer NOT NULL DEFAULT 0,
    status          varchar(32) NOT NULL,
    started_at      timestamptz NOT NULL DEFAULT now(),
    ended_at        timestamptz
);
CREATE INDEX idx_conversation_flow_sessions_active ON conversation_flow_sessions(conversation_id, status);
