package com.hardwareai.support.knowledge;

import jakarta.persistence.*;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "index_status")
    private IndexStatus indexStatus = IndexStatus.NOT_INDEXED;
    @Column(name = "content_checksum")
    private String contentChecksum;
    @Column(name = "parser_version")
    private String parserVersion;
    @Column(name = "failure_code")
    private String failureCode;
    @Column(name = "failure_detail")
    private String failureDetail;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected KnowledgeRevision() {
    }

    KnowledgeRevision(UUID doc, UUID product, String region) {
        this(doc, product, region, 1);
    }

    KnowledgeRevision(UUID doc, UUID product, String region, int revisionNo) {
        id = UUID.randomUUID();
        documentId = doc;
        this.revisionNo = revisionNo;
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

    public int revisionNo() { return revisionNo; }

    public IndexStatus indexStatus() { return indexStatus; }

    /**
     * Parser is the only path that writes normalized source text.
     */
    public void setExtractedText(
            String text
    ) {
        extractedText = text;
        contentChecksum = checksum(text);
        parserVersion = "structured-chunker-v1";
        status = Status.DRAFT;
        indexStatus = IndexStatus.NOT_INDEXED;
    }

    public void beginParsing() {
        if (status != Status.UPLOADED && status != Status.PARSING)
            throw new IllegalStateException("Only an uploaded revision can be parsed");
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
        indexStatus = IndexStatus.INDEXING;
        reviewedBy = user;
    }

    /** Index worker is the sole authority that makes a revision visible to retrieval. */
    public void markIndexedAndPublished() {
        if (status != Status.APPROVED || indexStatus != IndexStatus.INDEXING)
            throw new IllegalStateException("Only an approved indexing revision can be published");
        status = Status.PUBLISHED;
        indexStatus = IndexStatus.READY;
        publishedAt = Instant.now();
        failureCode = null;
        failureDetail = null;
    }

    public void markIndexFailed(String code) {
        if (indexStatus != IndexStatus.INDEXING) return;
        indexStatus = IndexStatus.FAILED;
        failureCode = code;
        failureDetail = null;
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
        indexStatus = IndexStatus.REMOVING;
    }

    /**
     * Restores an explicitly deprecated immutable revision; its content is never overwritten.
     */
    public void restore(UUID user) {
        if (status != Status.DEPRECATED) throw new IllegalStateException("Only deprecated knowledge can be restored");
        status = Status.APPROVED;
        indexStatus = IndexStatus.INDEXING;
        reviewedBy = user;
        publishedAt = Instant.now();
        deprecatedAt = null;
    }

    public void archive() {
        if (status != Status.PUBLISHED && status != Status.DEPRECATED)
            throw new IllegalStateException("Only published or deprecated revision can be archived");
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

    public enum IndexStatus { NOT_INDEXED, INDEXING, READY, FAILED, REMOVING }

    private static String checksum(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
