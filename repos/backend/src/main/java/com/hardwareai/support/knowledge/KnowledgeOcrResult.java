package com.hardwareai.support.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * OCR provenance kept separately from normalized revision text for review and parser upgrades.
 */
@Entity
@Table(name = "knowledge_ocr_results")
class KnowledgeOcrResult {
    @Id
    private UUID id;
    @Column(name = "revision_id")
    private UUID revisionId;
    @Column(name = "raw_text")
    private String rawText;
    @Column(name = "normalized_text")
    private String normalizedText;
    private Double confidence;
    private String language;
    @Column(name = "page_from")
    private Integer pageFrom;
    @Column(name = "page_to")
    private Integer pageTo;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected KnowledgeOcrResult() {
    }

    KnowledgeOcrResult(UUID revisionId, OcrClient.OcrText source, String normalizedText) {
        id = UUID.randomUUID();
        this.revisionId = revisionId;
        rawText = source.text();
        this.normalizedText = normalizedText;
        confidence = source.confidence();
        language = source.language();
        pageFrom = source.pageFrom();
        pageTo = source.pageTo();
    }
}
