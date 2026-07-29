package com.hardwareai.support.troubleshoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Immutable serialized definition captured when a flow is published. */
@Entity
@Table(name = "troubleshoot_flow_version_snapshots")
class TroubleshootFlowVersionSnapshot {
    @Id private UUID id;
    @Column(name = "flow_id") private UUID flowId;
    @Column(name = "version_no") private int versionNo;
    private String status;
    @Column(columnDefinition = "jsonb") private String definition;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();

    protected TroubleshootFlowVersionSnapshot() { }

    TroubleshootFlowVersionSnapshot(UUID flowId, int versionNo, String definition) {
        id = UUID.randomUUID();
        this.flowId = flowId;
        this.versionNo = versionNo;
        status = "PUBLISHED";
        this.definition = definition;
        publishedAt = Instant.now();
    }

    UUID id() { return id; }
    UUID flowId() { return flowId; }
    int versionNo() { return versionNo; }
    String definition() { return definition; }
}
