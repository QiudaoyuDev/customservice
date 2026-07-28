package com.hardwareai.support.knowledge;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** A publishable, auditable revision; state transitions deliberately reject bypasses. */
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

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  protected KnowledgeRevision() {}

  KnowledgeRevision(UUID doc, UUID product, String region) {
    id = UUID.randomUUID();
    documentId = doc;
    revisionNo = 1;
    productModelId = product;
    this.region = region;
    status = Status.DRAFT;
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

  /** Parser is the only path that writes normalized source text. */ public void setExtractedText(
    String text
  ) {
    extractedText = text;
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
    if (status != Status.REVIEW) throw new IllegalStateException(
      "Only a review revision can be published"
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

  public void archive() {
    if (status != Status.PUBLISHED) throw new IllegalStateException(
      "Only a published revision can be archived"
    );
    status = Status.ARCHIVED;
  }

  public enum Status {
    DRAFT,
    REVIEW,
    PUBLISHED,
    ARCHIVED,
  }
}
