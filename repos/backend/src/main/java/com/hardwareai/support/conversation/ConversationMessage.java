package com.hardwareai.support.conversation;
import jakarta.persistence.*;
import java.time.Instant; import java.util.UUID;
/** Immutable user or assistant message; attachment content never enters logs or this row. */
@Entity @Table(name="messages") class ConversationMessage {
 @Id private UUID id; @Column(name="conversation_id") private UUID conversationId; @Enumerated(EnumType.STRING) private Sender sender; @Column(columnDefinition="text") private String content; @Column(name="error_code") private String errorCode; @Enumerated(EnumType.STRING) private Status status; @Column(name="created_at") private Instant createdAt=Instant.now();
 protected ConversationMessage(){} ConversationMessage(UUID conversationId,String content,String errorCode){id=UUID.randomUUID();this.conversationId=conversationId;sender=Sender.USER;this.content=content;this.errorCode=errorCode;status=Status.RECEIVED;}
 UUID id(){return id;} String content(){return content;} String errorCode(){return errorCode;} Instant createdAt(){return createdAt;}
 enum Sender { USER, ASSISTANT, SYSTEM } enum Status { RECEIVED, PROCESSING, COMPLETED, CANCELLED }
}
