package com.hardwareai.support.handoff;

import org.springframework.stereotype.Component;

/** The default delivery channel: the durable HandoffRequest is the internal queue item. */
@Component
class InternalQueueAdapter implements HumanSupportAdapter {
    @Override public String channel() { return "INTERNAL"; }
    @Override public DeliveryResult create(HandoffDelivery delivery) {
        return new DeliveryResult(true, delivery.handoffId().toString(), null);
    }
}
