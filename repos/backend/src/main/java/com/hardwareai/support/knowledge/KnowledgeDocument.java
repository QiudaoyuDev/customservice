package com.hardwareai.support.knowledge;

import jakarta.persistence.*;

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
        UUID userId
    ) {
        id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
        this.locale = locale;
        objectKey = key;
        this.contentType = contentType;
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
