package com.hardwareai.support.product;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantTest {

    @Test
    void acceptsOnlyActiveVariantsWithinTheirValidityWindow() {
        Instant now = Instant.parse("2026-07-29T00:00:00Z");
        var active = new ProductVariant(UUID.randomUUID(), UUID.randomUUID(), "EU", "R2", "SKU-R2", now.minusSeconds(60), now.plusSeconds(60));
        var future = new ProductVariant(UUID.randomUUID(), UUID.randomUUID(), "EU", "R3", "SKU-R3", now.plusSeconds(1), null);

        assertThat(active.activeAt(now)).isTrue();
        assertThat(future.activeAt(now)).isFalse();
    }
}
