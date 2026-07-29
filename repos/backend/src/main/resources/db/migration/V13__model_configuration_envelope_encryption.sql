ALTER TABLE model_configurations RENAME COLUMN encrypted_api_key TO api_key_ciphertext;
ALTER TABLE model_configurations
    ADD COLUMN provider_type varchar(40) NOT NULL DEFAULT 'OPENAI_COMPATIBLE',
    ADD COLUMN vision_model varchar(160),
    ADD COLUMN timeout_ms integer NOT NULL DEFAULT 20000,
    ADD COLUMN temperature numeric(4,3) NOT NULL DEFAULT 0,
    ADD COLUMN max_tokens integer NOT NULL DEFAULT 800,
    ADD COLUMN is_default boolean NOT NULL DEFAULT false,
    ADD COLUMN api_key_nonce varchar(64),
    ADD COLUMN api_key_key_version varchar(32) NOT NULL DEFAULT 'v1';
CREATE UNIQUE INDEX uq_model_configurations_default_per_tenant ON model_configurations (tenant_id) WHERE is_default;
