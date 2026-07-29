package com.hardwareai.support.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A management-console identity. Tenant membership is immutable after creation
 * so every authenticated request can derive its data boundary server-side.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private final boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private final Instant createdAt = Instant.now();

    protected UserAccount() {
    }

    public UserAccount(UUID id, UUID tenantId, String email, String passwordHash, Role role) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public boolean enabled() {
        return enabled;
    }

    public enum Role {
        ADMIN,
        KNOWLEDGE_REVIEWER,
        ANALYST,
        SUPPORT_AGENT,
    }
}
