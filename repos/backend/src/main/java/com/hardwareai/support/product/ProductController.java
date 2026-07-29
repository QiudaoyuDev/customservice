package com.hardwareai.support.product;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped product administration APIs.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductApplicationService products;
    private final CurrentUser current;

    ProductController(ProductApplicationService p, CurrentUser c) {
        products = p;
        current = c;
    }

    @GetMapping
    public List<View> list() {
        return products
            .list(current.tenantId())
            .stream()
            .map(View::of)
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public View create(@Valid @RequestBody Create r) {
        var saved = products.create(current.tenantId(), r.family(), r.model(), r.displayName(), r.region(), r.hardwareVersion(),
            r.firmwareMin(), r.firmwareMax());
        log.info("Product created id={} tenant={} family={} model={} region={}", saved.id(), current.tenantId(), r.family(),
            r.model(), r.region());
        return View.of(saved);
    }

    record Create(
        @NotBlank @Size(max = 120) String family,
        @NotBlank @Size(max = 120) String model,
        @NotBlank @Size(max = 200) String displayName,
        @NotBlank @Size(max = 16) String region
        , @Size(max = 80) String hardwareVersion
        , @Size(max = 80) String firmwareMin
        , @Size(max = 80) String firmwareMax
    ) {
    }

    record View(
        UUID id,
        String family,
        String model,
        String displayName,
        String region,
        String hardwareVersion,
        String firmwareMin,
        String firmwareMax,
        String status
    ) {
        static View of(ProductModel p) {
            return new View(
                p.id(),
                p.family(),
                p.model(),
                p.displayName(),
                p.region(),
                p.hardwareVersion(),
                p.firmwareMin(),
                p.firmwareMax(),
                p.status().name()
            );
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public View update(@PathVariable UUID id, @Valid @RequestBody Create request) {
        return View.of(
            products.update(current.tenantId(), id, request.family(), request.model(), request.displayName(), request.region(),
                request.hardwareVersion(), request.firmwareMin(), request.firmwareMax()));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public void archive(@PathVariable UUID id) {
        products.archive(current.tenantId(), id);
        log.info("Product archived id={} tenant={}", id, current.tenantId());
    }

    @PostMapping("/{id}/aliases")
    @PreAuthorize("hasRole('ADMIN')")
    public void addAlias(@PathVariable UUID id, @Valid @RequestBody Alias request) {
        products.addAlias(current.tenantId(), id, request.alias());
        log.info("Product alias added product={} alias={}", id, request.alias());
    }

    record Alias(@NotBlank @Size(max = 120) String alias) {
    }

    @GetMapping("/{id}/variants")
    public List<VariantView> variants(@PathVariable UUID id) {
        return products.variants(current.tenantId(), id).stream().map(VariantView::of).toList();
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public VariantView createVariant(@PathVariable UUID id, @Valid @RequestBody CreateVariant request) {
        return VariantView.of(products.createVariant(current.tenantId(), id, request.region(), request.hardwareRevision(),
            request.sku(), request.validFrom(), request.validTo()));
    }

    @GetMapping("/{id}/variants/{variantId}/firmware")
    public List<FirmwareView> firmware(@PathVariable UUID id, @PathVariable UUID variantId) {
        return products.firmware(current.tenantId(), id, variantId).stream().map(FirmwareView::of).toList();
    }

    @PostMapping("/{id}/variants/{variantId}/firmware")
    @PreAuthorize("hasRole('ADMIN')")
    public FirmwareView createFirmware(@PathVariable UUID id, @PathVariable UUID variantId,
        @Valid @RequestBody CreateFirmware request) {
        return FirmwareView.of(products.createFirmware(current.tenantId(), id, variantId, request.version(),
            request.releaseDate(), request.checksum(), request.notes()));
    }

    record CreateVariant(@NotBlank @Size(max = 16) String region, @Size(max = 80) String hardwareRevision,
                         @Size(max = 120) String sku, Instant validFrom, Instant validTo) {
    }

    record VariantView(UUID id, String region, String hardwareRevision, String sku, String status) {
        static VariantView of(ProductVariant value) {
            return new VariantView(value.id(), value.region(), value.hardwareRevision(), value.sku(), value.status().name());
        }
    }

    record CreateFirmware(@NotBlank @Size(max = 80) String version, LocalDate releaseDate, @Size(max = 128) String checksum,
                          @Size(max = 2000) String notes) {
    }

    record FirmwareView(UUID id, String version, String status) {
        static FirmwareView of(FirmwareVersion value) {
            return new FirmwareView(value.id(), value.version(), value.status().name());
        }
    }
}
