package com.hardwareai.support.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductModel, UUID> {
    List<ProductModel> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<ProductModel> findByIdAndTenantId(UUID id, UUID tenantId);
}

interface ProductModelAliasRepository extends JpaRepository<ProductModelAlias, UUID> {
    List<ProductModelAlias> findAllByProductModelId(UUID productModelId);
}

interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findAllByTenantIdAndProductModelIdOrderByCreatedAtDesc(UUID tenantId, UUID productModelId);

    Optional<ProductVariant> findByIdAndTenantId(UUID id, UUID tenantId);
}

interface FirmwareVersionRepository extends JpaRepository<FirmwareVersion, UUID> {
    List<FirmwareVersion> findAllByProductVariantIdOrderByCreatedAtDesc(UUID productVariantId);
}
