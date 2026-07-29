ALTER TABLE answer_traces
    ADD COLUMN prompt_version varchar(80),
    ADD COLUMN finish_reason varchar(80),
    ADD COLUMN retrieval_count integer NOT NULL DEFAULT 0,
    ADD COLUMN selected_evidence_count integer NOT NULL DEFAULT 0;
CREATE INDEX idx_answer_citations_trace ON answer_citations (answer_trace_id);
