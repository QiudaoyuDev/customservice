CREATE TABLE handoff_notes
(
    id         uuid PRIMARY KEY,
    handoff_id uuid        NOT NULL REFERENCES handoff_requests(id),
    author_id  uuid        NOT NULL REFERENCES users(id),
    content    text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_handoff_notes_handoff ON handoff_notes(handoff_id, created_at);
