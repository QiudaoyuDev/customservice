CREATE TABLE troubleshoot_flow_definitions
(
    id         uuid PRIMARY KEY,
    tenant_id  uuid         NOT NULL REFERENCES tenants(id),
    title      varchar(200) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now()
);

ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS definition_id uuid;
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS version_no integer NOT NULL DEFAULT 1;

-- Pre-existing flow rows become the first version of their own stable definition.
INSERT INTO troubleshoot_flow_definitions (id, tenant_id, title, created_at)
SELECT id, tenant_id, title, created_at
FROM troubleshoot_flows
ON CONFLICT (id) DO NOTHING;

UPDATE troubleshoot_flows SET definition_id = id WHERE definition_id IS NULL;
ALTER TABLE troubleshoot_flows ALTER COLUMN definition_id SET NOT NULL;
ALTER TABLE troubleshoot_flows DROP CONSTRAINT IF EXISTS troubleshoot_flows_definition_id_fkey;
ALTER TABLE troubleshoot_flows ADD CONSTRAINT troubleshoot_flows_definition_id_fkey
    FOREIGN KEY (definition_id) REFERENCES troubleshoot_flow_definitions(id);
ALTER TABLE troubleshoot_flows ADD CONSTRAINT troubleshoot_flows_definition_version_key UNIQUE (definition_id, version_no);
CREATE INDEX idx_troubleshoot_flows_definition ON troubleshoot_flows(definition_id, version_no DESC);
