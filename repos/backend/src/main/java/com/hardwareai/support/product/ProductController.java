package com.hardwareai.support.product;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped product administration APIs.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository products;
    private final ProductModelAliasRepository aliases;
    private final CurrentUser current;

    ProductController(ProductRepository p, ProductModelAliasRepository a, CurrentUser c) {
        products = p;
        aliases = a;
        current = c;
    }

    @GetMapping
    public List<View> list() {
        return products
                .findAllByTenantIdOrderByCreatedAtDesc(current.tenantId())
                .stream()
                .map(View::of)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public View create(@Valid @RequestBody Create r) {
        var saved = products.save(
                new ProductModel(current.tenantId(), r.family(), r.model(), r.displayName(), r.region(), r.hardwareVersion(), r.firmwareMin(), r.firmwareMax())
        );
        log.info("Product created id={} tenant={} family={} model={} region={}", saved.id(), current.tenantId(), r.family(), r.model(), r.region());
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
        var product = products.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.update(request.family(), request.model(), request.displayName(), request.region(), request.hardwareVersion(), request.firmwareMin(), request.firmwareMax());
        return View.of(products.save(product));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public void archive(@PathVariable UUID id) {
        var product = products.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.archive();
        products.save(product);
        log.info("Product archived id={} tenant={}", id, current.tenantId());
    }

    @PostMapping("/{id}/aliases")
    @PreAuthorize("hasRole('ADMIN')")
    public void addAlias(@PathVariable UUID id, @Valid @RequestBody Alias request) {
        products.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        aliases.save(new ProductModelAlias(current.tenantId(), id, request.alias()));
        log.info("Product alias added product={} alias={}", id, request.alias());
    }

    record Alias(@NotBlank @Size(max = 120) String alias) {
    }
}
