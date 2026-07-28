ALTER TABLE product_models ADD COLUMN hardware_version varchar(80);
ALTER TABLE product_models ADD COLUMN firmware_min varchar(80);
ALTER TABLE product_models ADD COLUMN firmware_max varchar(80);

CREATE TABLE product_model_aliases (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants(id),
  product_model_id uuid NOT NULL REFERENCES product_models(id),
  alias varchar(120) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, alias)
);

ALTER TABLE qr_bindings ADD COLUMN revoked_at timestamptz;
ALTER TABLE qr_bindings ADD COLUMN revocation_reason varchar(300);

ALTER TABLE knowledge_revisions ADD COLUMN index_version integer NOT NULL DEFAULT 0;
ALTER TABLE knowledge_revisions ADD COLUMN hardware_version varchar(80);
ALTER TABLE knowledge_revisions ADD COLUMN firmware_min varchar(80);
ALTER TABLE knowledge_revisions ADD COLUMN firmware_max varchar(80);
ALTER TABLE knowledge_revisions ADD COLUMN effective_from timestamptz;
ALTER TABLE knowledge_revisions ADD COLUMN effective_to timestamptz;
ALTER TABLE knowledge_revisions ADD COLUMN deprecated_at timestamptz;

CREATE TABLE knowledge_chunks (
  id uuid PRIMARY KEY,
  revision_id uuid NOT NULL REFERENCES knowledge_revisions(id),
  chunk_no integer NOT NULL,
  page_no integer,
  heading varchar(500),
  content text NOT NULL,
  source_label varchar(600) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (revision_id, chunk_no)
);

CREATE INDEX idx_knowledge_chunks_revision ON knowledge_chunks(revision_id, chunk_no);
CREATE INDEX idx_knowledge_revision_scope ON knowledge_revisions(product_model_id, region, status, effective_from, effective_to);
