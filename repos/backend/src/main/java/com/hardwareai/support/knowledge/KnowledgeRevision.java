package com.hardwareai.support.knowledge;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A publishable, auditable revision; state transitions deliberately reject bypasses.
 */
@Entity
@Table(name = "knowledge_revisions")
public class KnowledgeRevision {

    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "revision_no")
    private int revisionNo;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "product_model_id")
    private UUID productModelId;

    private String region;

    @Column(name = "extracted_text")
    private String extractedText;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected KnowledgeRevision() {
    }

    KnowledgeRevision(UUID doc, UUID product, String region) {
        id = UUID.randomUUID();
        documentId = doc;
        revisionNo = 1;
        productModelId = product;
        this.region = region;
        status = Status.UPLOADED;
    }

    public UUID id() {
        return id;
    }

    public UUID documentId() {
        return documentId;
    }

    public Status status() {
        return status;
    }

    public UUID productModelId() {
        return productModelId;
    }

    public String region() {
        return region;
    }

    public String extractedText() {
        return extractedText;
    }

    /**
     * Parser is the only path that writes normalized source text.
     */
    public void setExtractedText(
        String text
    ) {
        extractedText = text;
        status = Status.DRAFT;
    }

    public void beginParsing() {
        if (status != Status.UPLOADED && status != Status.PARSING) throw new IllegalStateException("Only an uploaded revision can be parsed");
        status = Status.PARSING;
    }

    public void submit() {
        if (status != Status.DRAFT) throw new IllegalStateException(
            "Only a draft revision can be submitted"
        );
        if (extractedText == null || extractedText.isBlank()) throw new IllegalStateException(
            "The document must be parsed before review"
        );
        status = Status.REVIEW;
    }

    public void publish(UUID user) {
        if (status != Status.APPROVED) throw new IllegalStateException(
            "Only an approved revision can be published"
        );
        if (
            productModelId == null || region == null || region.isBlank()
        ) throw new IllegalStateException(
            "Published knowledge requires product and region applicability"
        );
        status = Status.PUBLISHED;
        reviewedBy = user;
        publishedAt = Instant.now();
    }

    public void approve(UUID user) {
        if (status != Status.REVIEW) throw new IllegalStateException("Only a review revision can be approved");
        reviewedBy = user;
        status = Status.APPROVED;
    }

    public void deprecate() {
        if (status != Status.PUBLISHED) throw new IllegalStateException("Only published knowledge can be deprecated");
        status = Status.DEPRECATED;
        deprecatedAt = Instant.now();
    }

    /** Restores an explicitly deprecated immutable revision; its content is never overwritten. */
    public void restore(UUID user) {
        if (status != Status.DEPRECATED) throw new IllegalStateException("Only deprecated knowledge can be restored");
        status = Status.PUBLISHED;
        reviewedBy = user;
        publishedAt = Instant.now();
        deprecatedAt = null;
    }

    public void archive() {
        if (status != Status.PUBLISHED && status != Status.DEPRECATED) throw new IllegalStateException("Only published or deprecated revision can be archived");
        status = Status.ARCHIVED;
    }

    public enum Status {
        UPLOADED,
        PARSING,
        DRAFT,
        REVIEW,
        APPROVED,
        PUBLISHED,
        DEPRECATED,
        ARCHIVED,
    }
}
