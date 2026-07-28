package com.hardwareai.support.qr;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Stores a one-way hash of a public QR credential, never the credential itself. */
@Entity
@Table(name = "qr_bindings")
public class QrBinding {

  @Id
  private UUID id;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "product_model_id")
  private UUID productModelId;

  @Column(name = "token_hash")
  private String tokenHash;

  private String batch;

  @Column(name = "serial_number")
  private String serialNumber;

  @Enumerated(EnumType.STRING)
  private Status status;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  protected QrBinding() {}

  QrBinding(
    UUID tenantId,
    UUID productModelId,
    String tokenHash,
    String batch,
    String serialNumber,
    Instant expiresAt
  ) {
    id = UUID.randomUUID();
    this.tenantId = tenantId;
    this.productModelId = productModelId;
    this.tokenHash = tokenHash;
    this.batch = batch;
    this.serialNumber = serialNumber;
    this.expiresAt = expiresAt;
    status = Status.ACTIVE;
  }

  public UUID id() {
    return id;
  }

  public UUID productModelId() {
    return productModelId;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public String batch() {
    return batch;
  }

  public String serialNumber() {
    return serialNumber;
  }

  public Status status() {
    return status;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  /** Public resolution must reject revoked and expired credentials. */ public boolean valid() {
    return status == Status.ACTIVE && (expiresAt == null || expiresAt.isAfter(Instant.now()));
  }

  public enum Status {
    ACTIVE,
    REVOKED,
  }
}
