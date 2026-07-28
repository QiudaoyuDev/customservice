package com.hardwareai.support.conversation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
/** Anonymous support session anchored only to a server-resolved QR product scope. */
@Entity @Table(name = "conversations")
class Conversation {
 @Id private UUID id; @Column(name="tenant_id") private UUID tenantId; @Column(name="qr_binding_id") private UUID qrBindingId;
 private String language, region; @Enumerated(EnumType.STRING) private Status status; @Column(name="created_at") private Instant createdAt=Instant.now(); @Column(name="closed_at") private Instant closedAt;
 protected Conversation(){} Conversation(UUID tenant, UUID qr, String language, String region){id=UUID.randomUUID();tenantId=tenant;qrBindingId=qr;this.language=language;this.region=region;status=Status.OPEN;}
 UUID id(){return id;} UUID tenantId(){return tenantId;} String language(){return language;} String region(){return region;} Status status(){return status;}
 enum Status { OPEN, CLOSED }
}
