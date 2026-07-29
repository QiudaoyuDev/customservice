package com.hardwareai.support.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Central tenant-scoped product write/read boundary used by controllers and anonymous QR flows. */
@Service
public class ProductApplicationService {
    private final ProductRepository products;
    private final ProductModelAliasRepository aliases;
    private final ProductVariantRepository variants;
    private final FirmwareVersionRepository firmwares;

    ProductApplicationService(ProductRepository products, ProductModelAliasRepository aliases, ProductVariantRepository variants, FirmwareVersionRepository firmwares) {
        this.products = products;
        this.aliases = aliases;
        this.variants = variants;
        this.firmwares = firmwares;
    }

    public ProductModel requireActiveProduct(UUID tenantId, UUID productId) {
        var product = products.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Product is unavailable"));
        if (product.status() != ProductModel.Status.ACTIVE) throw new IllegalStateException("Archived product cannot be used");
        return product;
    }

    public List<ProductModel> list(UUID tenantId) {
        return products.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<ProductModel> listActive(UUID tenantId) {
        return list(tenantId).stream().filter(product -> product.status() == ProductModel.Status.ACTIVE).toList();
    }

    @Transactional
    public ProductModel create(UUID tenantId, String family, String model, String displayName, String region,
                               String hardwareVersion, String firmwareMin, String firmwareMax) {
        return products.save(new ProductModel(tenantId, family, model, displayName, region, hardwareVersion, firmwareMin, firmwareMax));
    }

    @Transactional
    public ProductModel update(UUID tenantId, UUID productId, String family, String model, String displayName, String region,
                               String hardwareVersion, String firmwareMin, String firmwareMax) {
        var product = requireProduct(tenantId, productId);
        product.update(family, model, displayName, region, hardwareVersion, firmwareMin, firmwareMax);
        return products.save(product);
    }

    @Transactional
    public void archive(UUID tenantId, UUID productId) {
        var product = requireProduct(tenantId, productId);
        product.archive();
        products.save(product);
    }

    @Transactional
    public void addAlias(UUID tenantId, UUID productId, String alias) {
        requireProduct(tenantId, productId);
        aliases.save(new ProductModelAlias(tenantId, productId, alias));
    }

    public ProductModel requireProduct(UUID tenantId, UUID productId) {
        return products.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Product is unavailable"));
    }

    public ProductVariant requireActiveVariant(UUID tenantId, UUID productId, UUID variantId) {
        var variant = variants.findByIdAndTenantId(variantId, tenantId)
                .filter(v -> v.productModelId().equals(productId) && v.activeAt(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Product variant is unavailable"));
        requireActiveProduct(tenantId, productId);
        return variant;
    }

    @Transactional
    public ProductVariant createVariant(UUID tenantId, UUID productId, String region, String hardwareRevision, String sku,
                                        Instant validFrom, Instant validTo) {
        requireActiveProduct(tenantId, productId);
        if (validFrom != null && validTo != null && !validFrom.isBefore(validTo))
            throw new IllegalArgumentException("Variant validity range is invalid");
        return variants.save(new ProductVariant(tenantId, productId, region, hardwareRevision, sku, validFrom, validTo));
    }

    @Transactional
    public FirmwareVersion createFirmware(UUID tenantId, UUID productId, UUID variantId, String version,
                                          LocalDate releaseDate, String checksum, String notes) {
        requireActiveVariant(tenantId, productId, variantId);
        return firmwares.save(new FirmwareVersion(variantId, version, releaseDate, checksum, notes));
    }

    public List<ProductVariant> variants(UUID tenantId, UUID productId) {
        requireProduct(tenantId, productId);
        return variants.findAllByTenantIdAndProductModelIdOrderByCreatedAtDesc(tenantId, productId);
    }

    public List<FirmwareVersion> firmware(UUID tenantId, UUID productId, UUID variantId) {
        requireActiveVariant(tenantId, productId, variantId);
        return firmwares.findAllByProductVariantIdOrderByCreatedAtDesc(variantId);
    }
}
