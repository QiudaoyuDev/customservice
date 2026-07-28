package com.hardwareai.support.product;

import com.hardwareai.support.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tenant-scoped product administration APIs. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductRepository products;
  private final CurrentUser current;

  ProductController(ProductRepository p, CurrentUser c) {
    products = p;
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
    return View.of(
      products.save(
        new ProductModel(current.tenantId(), r.family(), r.model(), r.displayName(), r.region())
      )
    );
  }

  record Create(
    @NotBlank @Size(max = 120) String family,
    @NotBlank @Size(max = 120) String model,
    @NotBlank @Size(max = 200) String displayName,
    @NotBlank @Size(max = 16) String region
  ) {}

  record View(
    UUID id,
    String family,
    String model,
    String displayName,
    String region,
    String status
  ) {
    static View of(ProductModel p) {
      return new View(
        p.id(),
        p.family(),
        p.model(),
        p.displayName(),
        p.region(),
        p.status().name()
      );
    }
  }
}
