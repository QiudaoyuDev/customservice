package com.hardwareai.support.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareai.support.common.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Tenant-scoped, privacy-preserving operational overview; no customer message content is queried.
 */
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER','SUPPORT_AGENT')")
class AnalyticsController {
    private final OperationalEventRepository events;
    private final CurrentUser current;
    private final ObjectMapper json;

    AnalyticsController(OperationalEventRepository events, CurrentUser current, ObjectMapper json) {
        this.events = events;
        this.current = current;
        this.json = json;
    }

    @GetMapping("/overview")
    Overview overview(@RequestParam(defaultValue = "30") int days) {
        int boundedDays = Math.max(1, Math.min(days, 90));
        var rows = events.findAllByTenantIdAndCreatedAtAfter(current.tenantId(), Instant.now().minus(boundedDays, ChronoUnit.DAYS));
        var counts = rows.stream().collect(
            java.util.stream.Collectors.groupingBy(OperationalEvent::eventType, java.util.TreeMap::new,
                java.util.stream.Collectors.counting()));
        long answers = counts.getOrDefault("ANSWER_COMPLETED", 0L);
        long handoffs = rows.stream().filter(event -> "HANDOFF_CREATED".equals(event.eventType())).count();
        long noEvidence = rows.stream().filter(event -> attribute(event, "outcome").equals("NO_EVIDENCE")).count();
        long conflicts = rows.stream().filter(event -> attribute(event, "outcome").equals("CONFLICT")).count();
        double avgLatency = rows.stream().filter(event -> "ANSWER_COMPLETED".equals(event.eventType()))
            .mapToLong(event -> number(event, "latencyMs")).average().orElse(0);
        return new Overview(boundedDays, rows.size(), counts, answers, handoffs, noEvidence, conflicts, avgLatency);
    }

    private String attribute(OperationalEvent event, String key) {
        try {
            return json.readTree(event.attributes()).path(key).asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private long number(OperationalEvent event, String key) {
        try {
            return json.readTree(event.attributes()).path(key).asLong(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    record Overview(int days, int totalEvents, Map<String, Long> eventCounts, long answers, long handoffs, long noEvidence,
                    long conflicts, double averageAnswerLatencyMs) {
    }
}
