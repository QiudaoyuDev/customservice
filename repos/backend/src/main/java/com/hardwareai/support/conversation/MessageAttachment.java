package com.hardwareai.support.conversation;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
/** Private attachment reference; OCR may inspect it but clients never receive an object-store key. */
@Entity @Table(name="message_attachments") class MessageAttachment {
 @Id private UUID id; @Column(name="message_id") private UUID messageId; @Column(name="object_key") private String objectKey; @Column(name="content_type") private String contentType; @Column(name="size_bytes") private long sizeBytes; @Column(name="created_at") private Instant createdAt=Instant.now();
 protected MessageAttachment(){} MessageAttachment(UUID messageId,String key,String contentType,long size){id=UUID.randomUUID();this.messageId=messageId;objectKey=key;this.contentType=contentType;sizeBytes=size;}
 UUID id(){return id;}
}
