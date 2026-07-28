package com.hardwareai.support.conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface ConversationRepository extends JpaRepository<Conversation, UUID> { Optional<Conversation> findByIdAndTenantId(UUID id, UUID tenantId); }
interface ConversationProductContextRepository extends JpaRepository<ConversationProductContext, UUID> { List<ConversationProductContext> findAllByConversationIdAndActiveTrue(UUID conversationId); }
interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> { List<ConversationMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId); }
interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {}
interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, UUID> {}
