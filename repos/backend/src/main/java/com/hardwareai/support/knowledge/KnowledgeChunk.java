package com.hardwareai.support.knowledge;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Immutable retrieval unit retaining its source position and parent revision. */
@Entity
@Table(name = "knowledge_chunks")
class KnowledgeChunk {
    @Id private UUID id;
    @Column(name = "revision_id") private UUID revisionId;
    @Column(name = "chunk_no") private int chunkNo;
    @Column(name = "page_no") private Integer pageNo;
    private String heading;
    @Column(columnDefinition = "text") private String content;
    @Column(name = "source_label") private String sourceLabel;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    protected KnowledgeChunk() {}
    KnowledgeChunk(UUID revisionId, int chunkNo, String content, String sourceLabel) {
        this.id = UUID.randomUUID(); this.revisionId = revisionId; this.chunkNo = chunkNo;
        this.content = content; this.sourceLabel = sourceLabel;
    }
    UUID id() { return id; }
    int chunkNo() { return chunkNo; }
    String content() { return content; }
    String sourceLabel() { return sourceLabel; }
}
