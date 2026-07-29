ALTER TABLE handoff_requests ADD COLUMN priority varchar(32) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE handoff_requests ADD COLUMN sla_due_at timestamptz;
UPDATE handoff_requests SET sla_due_at = created_at + interval '48 hours' WHERE sla_due_at IS NULL;
ALTER TABLE handoff_requests ALTER COLUMN sla_due_at SET NOT NULL;
CREATE INDEX idx_handoff_requests_tenant_status_sla ON handoff_requests(tenant_id, status, sla_due_at);
