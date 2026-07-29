package com.hardwareai.support.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachment_analyses")
class AttachmentAnalysis {
    @Id
    private UUID id;
    @Column(name = "attachment_id")
    private UUID attachmentId;
    @Column(name = "ocr_text")
    private String ocrText;
    @Column(name = "error_code")
    private String errorCode;
    @Column(name = "indicator_description")
    private String indicatorDescription;
    private Double confidence;
    @Column(name = "requires_confirmation")
    private boolean requiresConfirmation = true;
    private String status;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected AttachmentAnalysis() {
    }

    AttachmentAnalysis(UUID attachmentId, String text, Double confidence) {
        id = UUID.randomUUID();
        this.attachmentId = attachmentId;
        ocrText = text;
        this.confidence = confidence;
        errorCode = findError(text);
        status = "READY";
    }

    UUID attachmentId() {
        return attachmentId;
    }

    String ocrText() {
        return ocrText;
    }

    String errorCode() {
        return errorCode;
    }

    Double confidence() {
        return confidence;
    }

    boolean requiresConfirmation() {
        return requiresConfirmation;
    }

    String status() {
        return status;
    }

    void confirm() {
        requiresConfirmation = false;
        status = "CONFIRMED";
    }

    private static String findError(String text) {
        var m = java.util.regex.Pattern.compile("\\b[A-Z]{1,4}[- ]?\\d{2,6}\\b").matcher(text == null ? "" : text);
        return m.find() ? m.group() : null;
    }
}
