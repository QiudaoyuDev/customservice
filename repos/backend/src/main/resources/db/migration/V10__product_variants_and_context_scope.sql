CREATE TABLE product_variants
(
    id                uuid PRIMARY KEY,
    tenant_id         uuid        NOT NULL REFERENCES tenants (id),
    product_model_id  uuid        NOT NULL REFERENCES product_models (id),
    region            varchar(16) NOT NULL,
    hardware_revision varchar(80),
    sku               varchar(120),
    status            varchar(32) NOT NULL,
    valid_from        timestamptz,
    valid_to          timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, product_model_id, region, hardware_revision, sku)
);

CREATE INDEX idx_product_variants_scope
    ON product_variants (tenant_id, product_model_id, region, status);

CREATE TABLE firmware_versions
(
    id                 uuid PRIMARY KEY,
    product_variant_id uuid         NOT NULL REFERENCES product_variants (id),
    version            varchar(80)  NOT NULL,
    release_date       date,
    status             varchar(32)  NOT NULL,
    checksum           varchar(128),
    notes              varchar(2000),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (product_variant_id, version)
);

ALTER TABLE qr_bindings
    ADD COLUMN product_variant_id uuid REFERENCES product_variants (id);
ALTER TABLE qr_bindings
    ADD COLUMN initial_firmware_version varchar(80);

ALTER TABLE conversation_product_contexts
    ADD COLUMN product_variant_id uuid REFERENCES product_variants (id);
ALTER TABLE conversation_product_contexts
    ADD COLUMN hardware_revision varchar(80);
ALTER TABLE conversation_product_contexts
    ADD COLUMN confirmed_by_user boolean NOT NULL DEFAULT false;
ALTER TABLE conversation_product_contexts
    ADD COLUMN closed_at timestamptz;

CREATE INDEX idx_conversation_product_context_active
    ON conversation_product_contexts (conversation_id, active, created_at);
