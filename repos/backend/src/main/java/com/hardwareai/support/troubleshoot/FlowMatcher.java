package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.llm.Intent;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Selects the most-specific published flow without trusting client-provided scope.
 */
@Service
public class FlowMatcher {
    private static final Pattern NUMERIC_PART = Pattern.compile("\\d+");
    private final TroubleshootFlowRepository flows;

    FlowMatcher(TroubleshootFlowRepository flows) {
        this.flows = flows;
    }

    public Optional<TroubleshootFlow> match(UUID tenantId, FlowMatchScope context, String region, String locale,
        Intent intent, String message, String errorCode) {
        return flows.findPublishedCandidates(tenantId, context.productModelId(), region, locale, intent).stream()
            .filter(flow -> appliesTo(flow, context, message, errorCode))
            .max(Comparator.comparingInt(TroubleshootFlow::priority)
                .thenComparingInt(this::specificity)
                .thenComparing(TroubleshootFlow::publishedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private boolean appliesTo(TroubleshootFlow flow, FlowMatchScope context, String message, String errorCode) {
        if (flow.productVariantId() != null && !flow.productVariantId().equals(context.productVariantId())) return false;
        if (flow.hardwareRevision() != null && !flow.hardwareRevision().isBlank()
            && !flow.hardwareRevision().equalsIgnoreCase(context.hardwareRevision())) return false;
        if (!withinFirmwareRange(context.firmwareVersion(), flow.firmwareMin(), flow.firmwareMax())) return false;
        if (flow.triggerPhrase() == null || flow.triggerPhrase().isBlank()) return true;
        String searchable = (message == null ? "" : message) + " " + (errorCode == null ? "" : errorCode);
        return searchable.toLowerCase(Locale.ROOT).contains(flow.triggerPhrase().toLowerCase(Locale.ROOT));
    }

    private int specificity(TroubleshootFlow flow) {
        return (flow.productVariantId() == null ? 0 : 4)
            + (blank(flow.hardwareRevision()) ? 0 : 2)
            + (blank(flow.firmwareMin()) && blank(flow.firmwareMax()) ? 0 : 1)
            + (blank(flow.triggerPhrase()) ? 0 : 1);
    }

    static boolean withinFirmwareRange(String firmware, String minimum, String maximum) {
        if (blank(minimum) && blank(maximum)) return true;
        if (blank(firmware)) return false;
        List<Integer> actual = parse(firmware);
        if (actual.isEmpty()) return false;
        return (blank(minimum) || compare(actual, parse(minimum)) >= 0)
            && (blank(maximum) || compare(actual, parse(maximum)) <= 0);
    }

    private static List<Integer> parse(String value) {
        return NUMERIC_PART.matcher(value).results().map(result -> Integer.parseInt(result.group())).toList();
    }

    private static int compare(List<Integer> left, List<Integer> right) {
        if (right.isEmpty()) return -1;
        int length = Math.max(left.size(), right.size());
        for (int i = 0; i < length; i++) {
            int l = i < left.size() ? left.get(i) : 0;
            int r = i < right.size() ? right.get(i) : 0;
            if (l != r) return Integer.compare(l, r);
        }
        return 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
