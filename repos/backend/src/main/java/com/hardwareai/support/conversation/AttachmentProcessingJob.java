package com.hardwareai.support.conversation;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "attachment_processing_jobs") class AttachmentProcessingJob {
 @Id private UUID id; @Column(name="attachment_id") private UUID attachmentId; private String status; private int attempts; @Column(name="error_code") private String errorCode; @Column(name="created_at") private Instant createdAt=Instant.now(); @Column(name="completed_at") private Instant completedAt;
 protected AttachmentProcessingJob(){} AttachmentProcessingJob(UUID attachmentId){id=UUID.randomUUID();this.attachmentId=attachmentId;status="PENDING";} UUID attachmentId(){return attachmentId;} void start(){status="RUNNING";attempts++;} void complete(){status="COMPLETED";completedAt=Instant.now();} void fail(Exception e){status=attempts>=3?"FAILED":"PENDING";errorCode=e.getClass().getSimpleName();}
}
