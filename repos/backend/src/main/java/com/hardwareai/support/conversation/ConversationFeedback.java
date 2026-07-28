package com.hardwareai.support.conversation;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
/** Explicit user outcome feeds later quality metrics without collecting identity by default. */
@Entity @Table(name="conversation_feedback") class ConversationFeedback {
 @Id private UUID id; @Column(name="conversation_id") private UUID conversationId; private boolean resolved; private String comment; @Column(name="created_at") private Instant createdAt=Instant.now();
 protected ConversationFeedback(){} ConversationFeedback(UUID conversationId,boolean resolved,String comment){id=UUID.randomUUID();this.conversationId=conversationId;this.resolved=resolved;this.comment=comment;}
}
