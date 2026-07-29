package com.hardwareai.support.conversation;

import com.hardwareai.support.knowledge.ObjectStorage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConversationRetentionServiceTest {
    @Test
    void removesObjectsBeforeDeletingAnExpiredClosedConversation() {
        var conversations = mock(ConversationRepository.class);
        var messages = mock(ConversationMessageRepository.class);
        var attachments = mock(MessageAttachmentRepository.class);
        var storage = mock(ObjectStorage.class);
        var conversation = mock(Conversation.class);
        var message = mock(ConversationMessage.class);
        var attachment = mock(MessageAttachment.class);
        var now = Instant.parse("2026-07-29T00:00:00Z");

        when(conversation.id()).thenReturn(UUID.randomUUID());
        when(message.id()).thenReturn(UUID.randomUUID());
        when(attachment.objectKey()).thenReturn("attachments/retained-object");
        when(conversations.findTop100ByStatusAndClosedAtBeforeOrderByClosedAtAsc(
                eq(Conversation.Status.CLOSED), eq(now.minus(Duration.ofDays(30)))))
                .thenReturn(List.of(conversation));
        when(messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.id())).thenReturn(List.of(message));
        when(attachments.findAllByMessageIdInOrderByCreatedAtAsc(List.of(message.id()))).thenReturn(List.of(attachment));

        new ConversationRetentionService(conversations, messages, attachments, storage, Duration.ofDays(30), java.time.Clock.systemUTC())
                .removeExpiredConversations(now);

        var order = inOrder(storage, conversations);
        order.verify(storage).delete("attachments/retained-object");
        order.verify(conversations).delete(conversation);
    }

    @Test
    void doesNothingWhenNoConversationHasReachedTheRetentionBoundary() {
        var conversations = mock(ConversationRepository.class);
        var messages = mock(ConversationMessageRepository.class);
        var attachments = mock(MessageAttachmentRepository.class);
        var storage = mock(ObjectStorage.class);
        when(conversations.findTop100ByStatusAndClosedAtBeforeOrderByClosedAtAsc(any(), any())).thenReturn(List.of());

        new ConversationRetentionService(conversations, messages, attachments, storage, Duration.ofDays(30), java.time.Clock.systemUTC())
                .removeExpiredConversations(Instant.parse("2026-07-29T00:00:00Z"));

        verifyNoInteractions(messages, attachments, storage);
        verify(conversations, never()).delete(any());
    }
}
