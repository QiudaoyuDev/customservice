package com.hardwareai.support.handoff;

/**
 * Optional delivery channel. The internal queue remains the authoritative work record.
 */
public interface HumanSupportAdapter {
    String channel();

    DeliveryResult create(HandoffDelivery delivery);

    default void handleWebhook(byte[] payload, String signature) {
        // Channels without callbacks deliberately do nothing.
    }

    record HandoffDelivery(java.util.UUID handoffId, String summary, String contact, boolean contactAuthorized,
                           String packageSnapshot) {
    }

    record DeliveryResult(boolean delivered, String externalConversationId, String errorCode) {
    }
}
