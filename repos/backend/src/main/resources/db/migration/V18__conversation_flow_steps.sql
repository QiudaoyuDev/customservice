CREATE TABLE conversation_flow_steps
(
    id               uuid PRIMARY KEY,
    flow_session_id  uuid         NOT NULL REFERENCES conversation_flow_sessions(id),
    node_key         varchar(100) NOT NULL,
    normalized_reply varchar(32)  NOT NULL,
    raw_message_id   uuid         NOT NULL REFERENCES messages(id),
    result           varchar(120) NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversation_flow_steps_session ON conversation_flow_steps(flow_session_id, created_at);
