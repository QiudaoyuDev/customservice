ALTER TABLE evaluation_cases ADD COLUMN IF NOT EXISTS product_model_id uuid REFERENCES product_models(id);
ALTER TABLE evaluation_cases ADD COLUMN IF NOT EXISTS product_variant_id uuid REFERENCES product_variants(id);
ALTER TABLE evaluation_cases ADD COLUMN IF NOT EXISTS hardware_revision varchar(80);
ALTER TABLE evaluation_cases ADD COLUMN IF NOT EXISTS firmware_version varchar(80);
