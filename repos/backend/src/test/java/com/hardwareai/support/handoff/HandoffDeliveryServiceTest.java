package com.hardwareai.support.handoff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class HandoffDeliveryServiceTest {
    @Test
    void preservesTheInternalRecordWhenAnOptionalChannelFails() {
        var internal = new InternalQueueAdapter();
        HumanSupportAdapter failing = new HumanSupportAdapter() {
            @Override public String channel() { return "OPTIONAL"; }
            @Override public DeliveryResult create(HandoffDelivery delivery) { throw new IllegalStateException("unavailable"); }
        };
        var request = new HandoffRequest(UUID.randomUUID(), UUID.randomUUID(), "key", "reason", "summary", null, false, "{}");

        assertDoesNotThrow(() -> new HandoffDeliveryService(List.of(internal, failing)).deliver(request));
    }
}
