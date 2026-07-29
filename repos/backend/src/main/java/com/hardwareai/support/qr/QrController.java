package com.hardwareai.support.qr;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
public class QrController {

    private static final Logger log = LoggerFactory.getLogger(QrController.class);

    private final QrApplicationService qr;
    private final CurrentUser current;

    QrController(QrApplicationService qr, CurrentUser c) {
        this.qr = qr;
        current = c;
    }

    @PostMapping("/api/qr-bindings")
    @PreAuthorize("hasRole('ADMIN')")
    public Created create(@Valid @RequestBody Create r) {
        var issued = qr.issue(current.tenantId(), r.productModelId(), r.productVariantId(), r.initialFirmwareVersion(),
                r.batch(), r.serialNumber(), r.expiresAt());
        log.info("QR binding created id={} tenant={} product={}", issued.binding().id(), current.tenantId(), r.productModelId());
        return new Created(issued.binding().id(), issued.token());
    }

    @PostMapping("/public/qr/resolve")
    public PublicContext resolve(@Valid @RequestBody Resolve r) {
        var resolved = qr.resolve(r.token());
        var b = resolved.binding();
        var p = resolved.product();
        log.info("QR resolved binding={} product={} region={}", b.id(), p.id(), p.region());
        return new PublicContext(p.displayName(), p.model(), resolved.variant() == null ? null : resolved.variant().hardwareRevision(),
                p.region(), b.batch());
    }

    @GetMapping("/api/qr-bindings")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<View> list() {
        return qr.list(current.tenantId()).stream().map(View::of).toList();
    }

    @PostMapping("/api/qr-bindings/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public void revoke(@PathVariable UUID id, @Valid @RequestBody Revoke request) {
        qr.revoke(current.tenantId(), id, request.reason());
        log.info("QR binding revoked id={} reasonLen={}", id, request.reason().length());
    }

    record Create(
            @NotNull UUID productModelId,
            UUID productVariantId,
            @Size(max = 80) String initialFirmwareVersion,
            @Size(max = 100) String batch,
            @Size(max = 100) String serialNumber,
            Instant expiresAt
    ) {
    }

    record Created(UUID id, String token) {
    }

    record Resolve(@NotBlank String token) {
    }

    record Revoke(@NotBlank @Size(max = 300) String reason) {
    }

    record View(UUID id, UUID productModelId, UUID productVariantId, String initialFirmwareVersion, String batch, String serialNumber, String status, Instant expiresAt) {
        static View of(QrBinding binding) {
            return new View(binding.id(), binding.productModelId(), binding.productVariantId(), binding.initialFirmwareVersion(), binding.batch(), binding.serialNumber(), binding.status().name(), binding.expiresAt());
        }
    }

    record PublicContext(
            String displayName,
            String model,
            String hardwareRevision,
            String region,
            String batch
    ) {
    }
}
