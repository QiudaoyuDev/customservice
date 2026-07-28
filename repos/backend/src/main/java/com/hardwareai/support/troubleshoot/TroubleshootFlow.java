package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.llm.Intent;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Auditable, publishable diagnostic flow. Lifecycle deliberately rejects bypasses,
 * mirroring KnowledgeRevision. The closed workflow vocabulary lives in TroubleshootTypes.
 */
@Entity
@Table(name = "troubleshoot_flows")
public class TroubleshootFlow {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    private String title;

    @Enumerated(EnumType.STRING)
    private Intent triggerIntent;

    @Column(name = "product_model_id")
    private UUID productModelId;

    private String region;

    private String locale;

    @Column(name = "firmware_min")
    private String firmwareMin;

    @Column(name = "firmware_max")
    private String firmwareMax;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String owner;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected TroubleshootFlow() {
    }

    TroubleshootFlow(UUID tenant, String title, Intent trigger, UUID product, String region, String locale) {
        id = UUID.randomUUID();
        tenantId = tenant;
        this.title = title;
        this.triggerIntent = trigger;
        this.productModelId = product;
        this.region = region;
        this.locale = locale;
        status = Status.DRAFT;
    }

    void update(String title, Intent trigger, UUID product, String region, String locale, String fwMin, String fwMax) {
        this.title = title;
        this.triggerIntent = trigger;
        this.productModelId = product;
        this.region = region;
        this.locale = locale;
        this.firmwareMin = fwMin;
        this.firmwareMax = fwMax;
    }

    void submit() {
        if (status != Status.DRAFT) throw new IllegalStateException("Only a draft flow can be submitted");
        status = Status.REVIEW;
    }

    void approve(UUID user) {
        if (status != Status.REVIEW) throw new IllegalStateException("Only a review flow can be approved");
        status = Status.APPROVED;
        owner = user.toString();
    }

    void publish(UUID user) {
        if (status != Status.APPROVED) throw new IllegalStateException("Only an approved flow can be published");
        if (productModelId == null || region == null || locale == null)
            throw new IllegalStateException("Published flow requires product, region and locale applicability");
        status = Status.PUBLISHED;
        owner = user.toString();
        publishedAt = Instant.now();
    }

    void deprecate() {
        if (status != Status.PUBLISHED) throw new IllegalStateException("Only a published flow can be deprecated");
        status = Status.DEPRECATED;
    }

    void restore(UUID user) {
        if (status != Status.DEPRECATED) throw new IllegalStateException("Only a deprecated flow can be restored");
        status = Status.PUBLISHED;
        owner = user.toString();
        publishedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String title() {
        return title;
    }

    public Intent triggerIntent() {
        return triggerIntent;
    }

    public UUID productModelId() {
        return productModelId;
    }

    public String region() {
        return region;
    }

    public String locale() {
        return locale;
    }

    public String firmwareMin() {
        return firmwareMin;
    }

    public String firmwareMax() {
        return firmwareMax;
    }

    public Status status() {
        return status;
    }

    public String owner() {
        return owner;
    }

    enum Status {
        DRAFT,
        REVIEW,
        APPROVED,
        PUBLISHED,
        DEPRECATED,
        ARCHIVED,
    }
}
