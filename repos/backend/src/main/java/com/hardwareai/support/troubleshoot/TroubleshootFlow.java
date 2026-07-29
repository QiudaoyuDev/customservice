package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.llm.Intent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "definition_id")
    private UUID definitionId;

    @Column(name = "version_no")
    private int versionNo = 1;

    private String title;

    @Enumerated(EnumType.STRING)
    private Intent triggerIntent;

    @Column(name = "product_model_id")
    private UUID productModelId;

    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "hardware_revision")
    private String hardwareRevision;

    private String region;

    private String locale;

    @Column(name = "firmware_min")
    private String firmwareMin;

    @Column(name = "firmware_max")
    private String firmwareMax;

    @Column(name = "trigger_phrase")
    private String triggerPhrase;

    private int priority;

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

    void update(String title, Intent trigger, UUID product, UUID variant, String hardwareRevision, String region, String locale,
        String fwMin, String fwMax, String triggerPhrase, int priority) {
        this.title = title;
        this.triggerIntent = trigger;
        this.productModelId = product;
        this.productVariantId = variant;
        this.hardwareRevision = hardwareRevision;
        this.region = region;
        this.locale = locale;
        this.firmwareMin = fwMin;
        this.firmwareMax = fwMax;
        this.triggerPhrase = triggerPhrase;
        this.priority = priority;
    }

    void assignDefinition(UUID value, int version) {
        definitionId = value;
        versionNo = version;
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

    public UUID definitionId() {
        return definitionId;
    }

    public int versionNo() {
        return versionNo;
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

    public UUID productVariantId() {
        return productVariantId;
    }

    public String hardwareRevision() {
        return hardwareRevision;
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

    public String triggerPhrase() {
        return triggerPhrase;
    }

    public int priority() {
        return priority;
    }

    public Status status() {
        return status;
    }

    public String owner() {
        return owner;
    }

    public Instant publishedAt() {
        return publishedAt;
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
