package com.hardwareai.support.qr;

import com.hardwareai.support.product.ProductApplicationService;
import com.hardwareai.support.product.ProductModel;
import com.hardwareai.support.product.ProductVariant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Issues and resolves opaque QR credentials without leaking tenant or internal identifiers in the token.
 */
@Service
public class QrApplicationService {
    private final QrBindingRepository bindings;
    private final ProductApplicationService products;

    QrApplicationService(QrBindingRepository bindings, ProductApplicationService products) {
        this.bindings = bindings;
        this.products = products;
    }

    @Transactional
    public Issued issue(UUID tenantId, UUID productId, UUID variantId, String initialFirmwareVersion,
        String batch, String serialNumber, Instant expiresAt) {
        ProductModel product = products.requireActiveProduct(tenantId, productId);
        ProductVariant variant = variantId == null ? null : products.requireActiveVariant(tenantId, product.id(), variantId);
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        var binding = bindings.save(new QrBinding(tenantId, product.id(), variant == null ? null : variant.id(),
            initialFirmwareVersion, hash(token), batch, serialNumber, expiresAt));
        return new Issued(binding, token);
    }

    public Resolved resolve(String token) {
        var binding = bindings.findByTokenHash(hash(token)).filter(QrBinding::valid)
            .orElseThrow(() -> new IllegalArgumentException("QR token is invalid or expired"));
        var product = products.requireActiveProduct(binding.tenantId(), binding.productModelId());
        ProductVariant variant = binding.productVariantId() == null ? null
            : products.requireActiveVariant(binding.tenantId(), product.id(), binding.productVariantId());
        return new Resolved(binding, product, variant);
    }

    public List<QrBinding> list(UUID tenantId) {
        return bindings.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void revoke(UUID tenantId, UUID bindingId, String reason) {
        var binding = bindings.findByIdAndTenantId(bindingId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("QR binding not found"));
        binding.revoke(reason);
        bindings.save(binding);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Issued(QrBinding binding, String token) {
    }

    public record Resolved(QrBinding binding, ProductModel product, ProductVariant variant) {
    }
}
