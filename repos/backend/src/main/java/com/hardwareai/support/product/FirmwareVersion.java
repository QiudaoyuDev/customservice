package com.hardwareai.support.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Firmware metadata is tenant-inherited from its variant and never supplied by an anonymous caller. */
@Entity
@Table(name = "firmware_versions")
public class FirmwareVersion {
    @Id
    private UUID id;
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;
    private String version;
    @Column(name = "release_date")
    private LocalDate releaseDate;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String checksum;
    private String notes;
    @Column(name = "created_at")
    private final Instant createdAt = Instant.now();

    protected FirmwareVersion() { }

    FirmwareVersion(UUID productVariantId, String version, LocalDate releaseDate, String checksum, String notes) {
        this.id = UUID.randomUUID();
        this.productVariantId = productVariantId;
        this.version = version;
        this.releaseDate = releaseDate;
        this.checksum = checksum;
        this.notes = notes;
        this.status = Status.RELEASED;
    }

    public UUID id() { return id; }
    public UUID productVariantId() { return productVariantId; }
    public String version() { return version; }
    public Status status() { return status; }
    public enum Status { RELEASED, RETIRED }
}
