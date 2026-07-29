package com.hardwareai.support.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {
    @Mock ProductRepository products;
    @Mock ProductModelAliasRepository aliases;
    @Mock ProductVariantRepository variants;
    @Mock FirmwareVersionRepository firmwares;

    @Test
    void rejectsArchivedProductsForNewQrOrConversationUse() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var product = new ProductModel(tenantId, "vacuum", "V1", "Vacuum V1", "EU", null, null, null);
        product.archive();
        when(products.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        var service = new ProductApplicationService(products, aliases, variants, firmwares);

        assertThatThrownBy(() -> service.requireActiveProduct(tenantId, productId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesVariantFromAnotherTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(variants.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());
        var service = new ProductApplicationService(products, aliases, variants, firmwares);

        assertThatThrownBy(() -> service.requireActiveVariant(tenantId, productId, variantId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
