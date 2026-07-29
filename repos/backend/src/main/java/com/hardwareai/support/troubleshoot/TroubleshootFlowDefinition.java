package com.hardwareai.support.troubleshoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable business identity shared by all draft and published revisions of a flow.
 */
@Entity
@Table(name = "troubleshoot_flow_definitions")
class TroubleshootFlowDefinition {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    private String title;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected TroubleshootFlowDefinition() {
    }

    TroubleshootFlowDefinition(UUID tenantId, String title) {
        id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
    }

    UUID id() {
        return id;
    }

    void rename(String value) {
        title = value;
    }
}
