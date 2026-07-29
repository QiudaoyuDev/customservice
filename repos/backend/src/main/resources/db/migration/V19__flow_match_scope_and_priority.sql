ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS product_variant_id uuid REFERENCES product_variants(id);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS hardware_revision varchar(80);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS trigger_phrase varchar(500);
ALTER TABLE troubleshoot_flows ADD COLUMN IF NOT EXISTS priority integer NOT NULL DEFAULT 0;

CREATE INDEX idx_troubleshoot_flows_published_scope
    ON troubleshoot_flows (tenant_id, product_model_id, region, locale, trigger_intent, status, priority DESC);
