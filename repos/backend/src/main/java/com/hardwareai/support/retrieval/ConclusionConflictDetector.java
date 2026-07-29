package com.hardwareai.support.retrieval;

import java.util.List;

/** Conservative guard for incompatible high-confidence operational instructions. */
final class ConclusionConflictDetector {
    private static final List<String> ANCHORS = List.of("charge", "power", "disassemble", "reset", "充电", "电源", "拆机", "复位");

    boolean conflicts(List<String> excerpts) {
        boolean restrictive = excerpts.stream().anyMatch(ConclusionConflictDetector::isRestrictive);
        if (!restrictive) return false;
        return excerpts.stream().anyMatch(ConclusionConflictDetector::isPermissive)
                && ANCHORS.stream().anyMatch(anchor -> excerpts.stream().filter(ConclusionConflictDetector::isRestrictive).anyMatch(text -> lower(text).contains(anchor))
                && excerpts.stream().filter(ConclusionConflictDetector::isPermissive).anyMatch(text -> lower(text).contains(anchor)));
    }

    private static boolean isRestrictive(String text) {
        String value = lower(text);
        return value.contains("must not") || value.contains("do not") || value.contains("never")
                || value.contains("禁止") || value.contains("不得") || value.contains("不要") || value.contains("严禁");
    }

    private static boolean isPermissive(String text) {
        String value = lower(text);
        return value.contains("may ") || value.contains("can ") || value.contains("safe to")
                || value.contains("可以") || value.contains("允许") || value.contains("建议");
    }

    private static String lower(String value) { return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT); }
}
