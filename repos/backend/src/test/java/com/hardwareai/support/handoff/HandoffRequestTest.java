package com.hardwareai.support.handoff;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandoffRequestTest {
    @Test
    void followsTheControlledInternalQueueLifecycle() {
        var request = new HandoffRequest(UUID.randomUUID(), UUID.randomUUID(), "key", "reason", "summary", null, false, "{}");

        request.claim(UUID.randomUUID());
        assertEquals(HandoffRequest.Status.ASSIGNED, request.status());
        request.transition(HandoffRequest.Status.IN_PROGRESS);
        request.transition(HandoffRequest.Status.WAITING_USER);
        request.transition(HandoffRequest.Status.IN_PROGRESS);
        request.transition(HandoffRequest.Status.RESOLVED);
        request.close(HandoffRequest.Resolution.RESOLVED);

        assertEquals(HandoffRequest.Status.CLOSED, request.status());
    }

    @Test
    void rejectsSkippingFromNewDirectlyToClosed() {
        var request = new HandoffRequest(UUID.randomUUID(), UUID.randomUUID(), "key", "reason", "summary", null, false, "{}");
        assertThrows(IllegalStateException.class, () -> request.transition(HandoffRequest.Status.CLOSED));
    }
}
