CREATE TABLE troubleshoot_flows
(
    id         uuid PRIMARY KEY,
    tenant_id  uuid         NOT NULL REFERENCES tenants (id),
    name       varchar(200) NOT NULL,
    status     varchar(32)  NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE TABLE troubleshoot_flow_versions
(
    id         uuid PRIMARY KEY,
    flow_id    uuid        NOT NULL REFERENCES troubleshoot_flows (id),
    version_no integer     NOT NULL,
    status     varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (flow_id, version_no)
);
CREATE TABLE troubleshoot_nodes
(
    id                 uuid PRIMARY KEY,
    flow_version_id    uuid         NOT NULL REFERENCES troubleshoot_flow_versions (id),
    node_key           varchar(100) NOT NULL,
    node_type          varchar(32)  NOT NULL,
    prompt             text         NOT NULL,
    expected_input     varchar(32),
    risk_level         varchar(32)  NOT NULL,
    source_revision_id uuid REFERENCES knowledge_revisions (id),
    next_yes           varchar(100),
    next_no            varchar(100),
    next_unknown       varchar(100),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (flow_version_id, node_key)
);
CREATE TABLE troubleshoot_sessions
(
    id               uuid PRIMARY KEY,
    conversation_id  uuid         NOT NULL REFERENCES conversations (id),
    flow_version_id  uuid         NOT NULL REFERENCES troubleshoot_flow_versions (id),
    current_node_key varchar(100) NOT NULL,
    status           varchar(32)  NOT NULL,
    failure_count    integer      NOT NULL DEFAULT 0,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    completed_at     timestamptz
);
