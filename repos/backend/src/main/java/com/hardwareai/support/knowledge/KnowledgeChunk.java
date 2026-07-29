package com.hardwareai.support.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Map;

/**
 * Immutable retrieval unit retaining its source position and parent revision.
 */
@Entity
@Table(name = "knowledge_chunks")
class KnowledgeChunk {
    @Id
    private UUID id;
    @Column(name = "revision_id")
    private UUID revisionId;
    @Column(name = "chunk_no")
    private int chunkNo;
    @Column(name = "page_no")
    private Integer pageNo;
    private String heading;
    @Column(name = "title_path")
    private String titlePath;
    @Column(name = "page_from")
    private Integer pageFrom;
    @Column(name = "page_to")
    private Integer pageTo;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "source_label")
    private String sourceLabel;
    @Column(name = "content_checksum")
    private String contentChecksum;
    @Column(name = "token_count")
    private Integer tokenCount;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected KnowledgeChunk() {
    }

    KnowledgeChunk(UUID revisionId, int chunkNo, String content, String sourceLabel) {
        this(revisionId, chunkNo, content, sourceLabel, sourceLabel, null, null);
    }

    KnowledgeChunk(UUID revisionId, int chunkNo, String content, String sourceLabel, String titlePath, Integer pageFrom, Integer pageTo) {
        this.id = UUID.randomUUID();
        this.revisionId = revisionId;
        this.chunkNo = chunkNo;
        this.content = content;
        this.sourceLabel = sourceLabel;
        this.titlePath = titlePath;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.pageNo = pageFrom;
        this.heading = titlePath;
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("titlePath", titlePath);
        if (pageFrom != null) data.put("pageFrom", pageFrom);
        if (pageTo != null) data.put("pageTo", pageTo);
        metadata = Map.copyOf(data);
        this.contentChecksum = checksum(content);
        this.tokenCount = content.isBlank() ? 0 : content.trim().split("\\s+").length;
    }

    UUID id() {
        return id;
    }

    int chunkNo() {
        return chunkNo;
    }

    String content() {
        return content;
    }

    String sourceLabel() {
        return sourceLabel;
    }

    String titlePath() { return titlePath; }
    Integer pageFrom() { return pageFrom; }

    private static String checksum(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
