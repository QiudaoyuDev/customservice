CREATE TABLE tenants (
  id uuid PRIMARY KEY,
  name varchar(160) NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants (id),
  email varchar(320) NOT NULL,
  password_hash varchar(100) NOT NULL,
  role varchar(32) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, email)
);

CREATE TABLE product_models (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants (id),
  family varchar(120) NOT NULL,
  model varchar(120) NOT NULL,
  display_name varchar(200) NOT NULL,
  region varchar(16) NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_models_scope ON product_models (tenant_id, model, region);

CREATE TABLE qr_bindings (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants (id),
  product_model_id uuid NOT NULL REFERENCES product_models (id),
  token_hash varchar(128) NOT NULL UNIQUE,
  batch varchar(100),
  serial_number varchar(100),
  status varchar(32) NOT NULL,
  expires_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_documents (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants (id),
  title varchar(300) NOT NULL,
  locale varchar(16) NOT NULL,
  object_key varchar(500) NOT NULL,
  content_type varchar(120) NOT NULL,
  status varchar(32) NOT NULL,
  created_by uuid NOT NULL REFERENCES users (id),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_revisions (
  id uuid PRIMARY KEY,
  document_id uuid NOT NULL REFERENCES knowledge_documents (id),
  revision_no integer NOT NULL,
  status varchar(32) NOT NULL,
  product_model_id uuid REFERENCES product_models (id),
  region varchar(16),
  extracted_text text,
  reviewed_by uuid REFERENCES users (id),
  published_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (document_id, revision_no)
);

CREATE TABLE index_jobs (
  id uuid PRIMARY KEY,
  revision_id uuid NOT NULL REFERENCES knowledge_revisions (id),
  status varchar(32) NOT NULL,
  error_message text,
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants (id),
  actor_id uuid REFERENCES users (id),
  action varchar(80) NOT NULL,
  resource_type varchar(80) NOT NULL,
  resource_id uuid,
  details jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);