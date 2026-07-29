INSERT INTO knowledge_revision_applicability
    (id, revision_id, product_model_id, region, hardware_revision, firmware_min, firmware_max, valid_from, valid_to)
SELECT gen_random_uuid(), id, product_model_id, region, hardware_version, firmware_min, firmware_max,
       effective_from, effective_to
FROM knowledge_revisions
WHERE product_model_id IS NOT NULL
  AND region IS NOT NULL;
