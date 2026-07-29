-- V6 predates the current flow aggregate.  Preserve its historical rows while
-- adding the columns used by the published-flow and immutable-session model.
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS current_flow_id uuid;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS current_node_key varchar(100);
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS flow_failures integer NOT NULL DEFAULT 0;

ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS title varchar(200);
UPDATE troubleshoot_flows SET title = name WHERE title IS NULL;
ALTER TABLE troubleshoot_flows ALTER COLUMN title SET NOT NULL;
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS trigger_intent varchar(32) NOT NULL DEFAULT 'TROUBLESHOOTING';
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS product_model_id uuid;
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS region varchar(16);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS locale varchar(16);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS firmware_min varchar(80);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS firmware_max varchar(80);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS owner varchar(120);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS published_at timestamptz;

ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS flow_id uuid;
UPDATE troubleshoot_nodes node
SET flow_id = version.flow_id
FROM troubleshoot_flow_versions version
WHERE node.flow_version_id = version.id
  AND node.flow_id IS NULL;
ALTER TABLE troubleshoot_nodes ALTER COLUMN flow_id SET NOT NULL;
ALTER TABLE troubleshoot_nodes DROP CONSTRAINT IF EXISTS troubleshoot_nodes_flow_id_fkey;
ALTER TABLE troubleshoot_nodes ADD CONSTRAINT troubleshoot_nodes_flow_id_fkey
    FOREIGN KEY (flow_id) REFERENCES troubleshoot_flows(id);
ALTER TABLE troubleshoot_nodes ALTER COLUMN flow_version_id DROP NOT NULL;
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS risk varchar(32) NOT NULL DEFAULT 'LOW';
UPDATE troubleshoot_nodes SET risk = COALESCE(risk_level, 'LOW') WHERE risk IS NULL OR risk = 'LOW';
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS branch_yes varchar(100);
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS branch_no varchar(100);
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS branch_unknown varchar(100);
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS branch_next varchar(100);
UPDATE troubleshoot_nodes SET branch_yes = next_yes, branch_no = next_no, branch_unknown = next_unknown
WHERE branch_yes IS NULL AND branch_no IS NULL AND branch_unknown IS NULL;
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS safety_stop boolean NOT NULL DEFAULT false;
ALTER TABLE troubleshoot_nodes ADD COLUMN IF NOT EXISTS order_index integer NOT NULL DEFAULT 0;
ALTER TABLE troubleshoot_nodes DROP CONSTRAINT IF EXISTS troubleshoot_nodes_flow_version_id_node_key_key;
ALTER TABLE troubleshoot_nodes ADD CONSTRAINT troubleshoot_nodes_flow_id_node_key_key UNIQUE (flow_id, node_key);

CREATE TABLE IF NOT EXISTS troubleshoot_node_refs
(
    node_id    uuid         NOT NULL REFERENCES troubleshoot_nodes(id),
    source_ref varchar(500) NOT NULL
);

-- Existing published flows must also receive a fixed definition before a new
-- customer message is allowed to resume them.
INSERT INTO troubleshoot_flow_version_snapshots (id, flow_id, version_no, status, definition, published_at, created_at)
SELECT
    (substr(md5(flow.id::text || ':snapshot:1'), 1, 8) || '-' || substr(md5(flow.id::text || ':snapshot:1'), 9, 4) || '-' ||
     substr(md5(flow.id::text || ':snapshot:1'), 13, 4) || '-' || substr(md5(flow.id::text || ':snapshot:1'), 17, 4) || '-' ||
     substr(md5(flow.id::text || ':snapshot:1'), 21, 12))::uuid,
    flow.id,
    1,
    'PUBLISHED',
    jsonb_build_object('nodes', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'nodeKey', node.node_key,
            'nodeType', node.node_type,
            'prompt', node.prompt,
            'risk', node.risk,
            'expectedInput', node.expected_input,
            'branchYes', node.branch_yes,
            'branchNo', node.branch_no,
            'branchUnknown', node.branch_unknown,
            'branchNext', node.branch_next,
            'safetyStop', node.safety_stop,
            'sourceRefs', COALESCE((SELECT jsonb_agg(reference.source_ref) FROM troubleshoot_node_refs reference WHERE reference.node_id = node.id), '[]'::jsonb),
            'orderIndex', node.order_index
        ) ORDER BY node.order_index)
        FROM troubleshoot_nodes node
        WHERE node.flow_id = flow.id
    ), '[]'::jsonb)),
    COALESCE(flow.published_at, flow.created_at),
    now()
FROM troubleshoot_flows flow
WHERE flow.status = 'PUBLISHED'
  AND NOT EXISTS (SELECT 1 FROM troubleshoot_flow_version_snapshots snapshot WHERE snapshot.flow_id = flow.id);
