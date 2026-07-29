package com.hardwareai.support.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    private String title, locale;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "source_checksum")
    private String sourceChecksum;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected KnowledgeDocument() {
    }

    KnowledgeDocument(
        UUID tenantId,
        String title,
        String locale,
        String key,
        String contentType,
        UUID userId, String sourceChecksum
    ) {
        id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
        this.locale = locale;
        objectKey = key;
        this.contentType = contentType;
        this.sourceChecksum = sourceChecksum;
        createdBy = userId;
        status = Status.DRAFT;
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

    public String locale() {
        return locale;
    }

    public String objectKey() {
        return objectKey;
    }

    public String contentType() {
        return contentType;
    }

    public String sourceChecksum() {
        return sourceChecksum;
    }

    public Status status() {
        return status;
    }

    public enum Status {
        DRAFT,
        REVIEW,
        PUBLISHED,
        ARCHIVED,
    }
}
