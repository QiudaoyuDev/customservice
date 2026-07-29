package com.hardwareai.support.troubleshoot;

import java.util.UUID;

/**
 * Server-resolved product scope supplied to the diagnostic flow matcher.
 */
public record FlowMatchScope(UUID productModelId, UUID productVariantId, String hardwareRevision, String firmwareVersion) {
}
