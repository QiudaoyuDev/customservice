package com.hardwareai.support.conversation;

import com.hardwareai.support.product.ProductApplicationService;
import com.hardwareai.support.product.ProductModel;
import com.hardwareai.support.product.ProductVariant;
import com.hardwareai.support.qr.QrApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

/** Owns context replacement so product changes retain audit history and always terminate a running flow. */
@Service
public class ConversationContextService {
    private final ConversationProductContextRepository contexts;
    private final ProductApplicationService products;

    ConversationContextService(ConversationProductContextRepository contexts, ProductApplicationService products) {
        this.contexts = contexts;
        this.products = products;
    }

    @Transactional
    public void establishFromQr(Conversation conversation, QrApplicationService.Resolved resolved,
                                String reportedHardware, String reportedFirmware) {
        var binding = resolved.binding();
        ProductVariant variant = resolved.variant();
        String hardware = variant == null ? reportedHardware : variant.hardwareRevision();
        String firmware = binding.initialFirmwareVersion() == null ? reportedFirmware : binding.initialFirmwareVersion();
        contexts.save(new ConversationProductContext(conversation.id(), binding.productModelId(), binding.productVariantId(),
                variant == null ? null : variant.hardwareRevision(), hardware, firmware, "QR", false));
    }

    @Transactional
    public void replaceByUser(Conversation conversation, UUID productId, UUID variantId, String hardwareRevision,
                              String firmwareVersion) {
        var product = products.requireActiveProduct(conversation.tenantId(), productId);
        ProductVariant variant = variantId == null ? null : products.requireActiveVariant(conversation.tenantId(), product.id(), variantId);
        if (variant != null && !variant.region().equals(conversation.region()))
            throw new IllegalArgumentException("Selected product variant is not available in this region");
        contexts.findAllByConversationIdAndActiveTrue(conversation.id()).forEach(context -> {
            context.close();
            contexts.save(context);
        });
        contexts.save(new ConversationProductContext(conversation.id(), product.id(), variant == null ? null : variant.id(),
                variant == null ? hardwareRevision : variant.hardwareRevision(), hardwareRevision, firmwareVersion,
                "USER_SELECTED", true));
        conversation.clearFlow();
    }

    public List<ProductModel> selectableProducts(Conversation conversation) {
        return products.listActive(conversation.tenantId()).stream()
                .filter(product -> product.region().equals(conversation.region()))
                .toList();
    }
}
