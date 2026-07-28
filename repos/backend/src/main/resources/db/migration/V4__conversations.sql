CREATE TABLE conversations (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES tenants(id),
  qr_binding_id uuid REFERENCES qr_bindings(id),
  language varchar(16) NOT NULL,
  region varchar(16) NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  closed_at timestamptz
);
CREATE TABLE conversation_product_contexts (
  id uuid PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id),
  product_model_id uuid NOT NULL REFERENCES product_models(id),
  hardware_version varchar(80),
  firmware_version varchar(80),
  source varchar(32) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_active_conversation_context ON conversation_product_contexts(conversation_id) WHERE active;
CREATE TABLE messages (
  id uuid PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id),
  sender varchar(16) NOT NULL,
  content text NOT NULL,
  error_code varchar(100),
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE message_attachments (
  id uuid PRIMARY KEY,
  message_id uuid NOT NULL REFERENCES messages(id),
  object_key varchar(500) NOT NULL,
  content_type varchar(120) NOT NULL,
  size_bytes bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE conversation_feedback (
  id uuid PRIMARY KEY,
  conversation_id uuid NOT NULL REFERENCES conversations(id),
  resolved boolean NOT NULL,
  comment varchar(1000),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
