package com.hardwareai.support.conversation;

import jakarta.persistence.*;
import com.hardwareai.support.troubleshoot.TroubleshootTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable user or assistant message; attachment content never enters logs or this row.
 */
@Entity
@Table(name = "messages")
class ConversationMessage {
    @Id
    private UUID id;
    @Column(name = "conversation_id")
    private UUID conversationId;
    @Enumerated(EnumType.STRING)
    private Sender sender;
    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "error_code")
    private String errorCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "controlled_reply")
    private TroubleshootTypes.Reply controlledReply;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected ConversationMessage() {
    }

    ConversationMessage(UUID conversationId, String content, String errorCode) {
        this(conversationId, content, errorCode, null);
    }

    ConversationMessage(UUID conversationId, String content, String errorCode, TroubleshootTypes.Reply controlledReply) {
        id = UUID.randomUUID();
        this.conversationId = conversationId;
        sender = Sender.USER;
        this.content = content;
        this.errorCode = errorCode;
        this.controlledReply = controlledReply;
        status = Status.RECEIVED;
    }

    static ConversationMessage assistant(UUID conversationId, String content) {
        var message = new ConversationMessage();
        message.id = UUID.randomUUID();
        message.conversationId = conversationId;
        message.sender = Sender.ASSISTANT;
        message.content = content;
        message.status = Status.COMPLETED;
        return message;
    }

    UUID id() {
        return id;
    }

    String content() {
        return content;
    }

    String errorCode() {
        return errorCode;
    }
    TroubleshootTypes.Reply controlledReply() { return controlledReply; }
    UUID conversationId() { return conversationId; }

    Sender sender() { return sender; }

    Instant createdAt() {
        return createdAt;
    }

    enum Sender {USER, ASSISTANT, SYSTEM}

    enum Status {RECEIVED, PROCESSING, COMPLETED, CANCELLED}
}
