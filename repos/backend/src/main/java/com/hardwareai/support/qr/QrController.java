package com.hardwareai.support.qr;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.product.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@RestController
public class QrController {

    private final QrBindingRepository bindings;
    private final ProductRepository products;
    private final CurrentUser current;

    QrController(QrBindingRepository b, ProductRepository p, CurrentUser c) {
        bindings = b;
        products = p;
        current = c;
    }

    @PostMapping("/api/qr-bindings")
    @PreAuthorize("hasRole('ADMIN')")
    public Created create(@Valid @RequestBody Create r) {
        var p = products
            .findByIdAndTenantId(r.productModelId(), current.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        var token = UUID.randomUUID() + "." + UUID.randomUUID();
        var b = bindings.save(
            new QrBinding(
                current.tenantId(),
                p.id(),
                hash(token),
                r.batch(),
                r.serialNumber(),
                r.expiresAt()
            )
        );
        return new Created(b.id(), token);
    }

    @PostMapping("/public/qr/resolve")
    public PublicContext resolve(@Valid @RequestBody Resolve r) {
        var b = bindings
            .findByTokenHash(hash(r.token()))
            .filter(QrBinding::valid)
            .orElseThrow(() -> new IllegalArgumentException("QR token is invalid or expired"));
        var p = products
            .findById(b.productModelId())
            .orElseThrow(() -> new IllegalArgumentException("Product is unavailable"));
        return new PublicContext(p.id(), p.displayName(), p.model(), p.region(), b.batch());
    }

    @GetMapping("/api/qr-bindings")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<View> list() {
        return bindings.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(View::of).toList();
    }

    @PostMapping("/api/qr-bindings/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public void revoke(@PathVariable UUID id, @Valid @RequestBody Revoke request) {
        var binding = bindings.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("QR binding not found"));
        binding.revoke(request.reason()); bindings.save(binding);
    }

    private static String hash(String s) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    record Create(
        @NotNull UUID productModelId,
        @Size(max = 100) String batch,
        @Size(max = 100) String serialNumber,
        Instant expiresAt
    ) {
    }

    record Created(UUID id, String token) {
    }

    record Resolve(@NotBlank String token) {
    }
    record Revoke(@NotBlank @Size(max = 300) String reason) {}
    record View(UUID id, UUID productModelId, String batch, String serialNumber, String status, Instant expiresAt) {
        static View of(QrBinding binding) { return new View(binding.id(), binding.productModelId(), binding.batch(), binding.serialNumber(), binding.status().name(), binding.expiresAt()); }
    }

    record PublicContext(
        UUID productModelId,
        String displayName,
        String model,
        String region,
        String batch
    ) {
    }
}
