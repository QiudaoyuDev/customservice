package com.hardwareai.support.conversation;

import com.hardwareai.support.knowledge.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Removes closed anonymous-support sessions after the configured retention period.
 * Objects are deleted before their database rows, so a storage outage leaves the
 * conversation intact for a later retry instead of silently orphaning an attachment.
 */
@Service
class ConversationRetentionService {
    private static final Logger log = LoggerFactory.getLogger(ConversationRetentionService.class);

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final ObjectStorage storage;
    private final Clock clock;
    private final Duration retention;

    @Autowired
    ConversationRetentionService(
        ConversationRepository conversations,
        ConversationMessageRepository messages,
        MessageAttachmentRepository attachments,
        ObjectStorage storage,
        @Value("${app.retention.closed-conversation:PT720H}") Duration retention
    ) {
        this(conversations, messages, attachments, storage, retention, Clock.systemUTC());
    }

    ConversationRetentionService(
        ConversationRepository conversations,
        ConversationMessageRepository messages,
        MessageAttachmentRepository attachments,
        ObjectStorage storage,
        Duration retention,
        Clock clock
    ) {
        this.conversations = conversations;
        this.messages = messages;
        this.attachments = attachments;
        this.storage = storage;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.retention.cleanup-cron:0 30 3 * * *}")
    void removeExpiredConversations() {
        removeExpiredConversations(Instant.now(clock));
    }

    @Transactional
    void removeExpiredConversations(Instant now) {
        var expired = conversations.findTop100ByStatusAndClosedAtBeforeOrderByClosedAtAsc(
            Conversation.Status.CLOSED, now.minus(retention));
        for (var conversation : expired) {
            var messageIds = messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.id()).stream()
                .map(ConversationMessage::id)
                .toList();
            for (var attachment : attachments.findAllByMessageIdInOrderByCreatedAtAsc(messageIds)) {
                storage.delete(attachment.objectKey());
            }
            conversations.delete(conversation);
            log.info("Deleted expired closed conversation {}", conversation.id());
        }
    }
}
