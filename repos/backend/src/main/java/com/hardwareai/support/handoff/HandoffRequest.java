package com.hardwareai.support.handoff;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
/** Durable human-support work item; notification retries never create a second request. */
@Entity @Table(name="handoff_requests") class HandoffRequest {
 @Id private UUID id; @Column(name="tenant_id") private UUID tenantId; @Column(name="conversation_id") private UUID conversationId; @Column(name="idempotency_key") private String idempotencyKey; @Enumerated(EnumType.STRING) private Status status; private String reason; @Column(columnDefinition="text") private String summary; @Column(name="contact_authorized") private boolean contactAuthorized; @Column(name="assigned_to") private UUID assignedTo; @Enumerated(EnumType.STRING) private Resolution resolution; @Column(name="created_at") private Instant createdAt=Instant.now(); @Column(name="closed_at") private Instant closedAt;
 protected HandoffRequest(){} HandoffRequest(UUID tenant,UUID conversation,String key,String reason,String summary,boolean contact){id=UUID.randomUUID();tenantId=tenant;conversationId=conversation;idempotencyKey=key;this.reason=reason;this.summary=summary;contactAuthorized=contact;status=Status.NEW;}
 UUID id(){return id;} UUID tenantId(){return tenantId;} void claim(UUID user){if(status!=Status.NEW)throw new IllegalStateException("Only new requests can be claimed");status=Status.IN_PROGRESS;assignedTo=user;} void close(Resolution resolution){if(status!=Status.IN_PROGRESS)throw new IllegalStateException("Request must be claimed before close");status=Status.CLOSED;this.resolution=resolution;closedAt=Instant.now();}
 enum Status{NEW,IN_PROGRESS,CLOSED} enum Resolution{RESOLVED,WAITING_PARTS,WARRANTY,ABANDONED,DUPLICATE,PRODUCT_DEFECT}
}
