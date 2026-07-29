package com.hardwareai.support.handoff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Best-effort fan-out that never rolls back the internal queue when an optional channel is unavailable. */
@Service
class HandoffDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(HandoffDeliveryService.class);
    private final List<HumanSupportAdapter> adapters;

    HandoffDeliveryService(List<HumanSupportAdapter> adapters) { this.adapters = adapters; }

    void deliver(HandoffRequest request) {
        var delivery = new HumanSupportAdapter.HandoffDelivery(request.id(), request.summary(), request.contact(),
                request.contactAuthorized(), request.packageSnapshot());
        for (var adapter : adapters) {
            try {
                var result = adapter.create(delivery);
                if (result.delivered()) log.info("Handoff delivered handoff={} channel={} externalId={}", request.id(), adapter.channel(), result.externalConversationId());
                else log.warn("Handoff delivery rejected handoff={} channel={} code={}", request.id(), adapter.channel(), result.errorCode());
            } catch (Exception exception) {
                log.warn("Handoff delivery failed handoff={} channel={}", request.id(), adapter.channel());
            }
        }
    }
}
