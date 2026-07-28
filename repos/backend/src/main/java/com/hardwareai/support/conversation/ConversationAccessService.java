package com.hardwareai.support.conversation;
import org.springframework.stereotype.Service; import java.util.UUID;
/** Exposes only the tenant boundary required by downstream domain services. */
@Service public class ConversationAccessService { private final ConversationRepository conversations; public ConversationAccessService(ConversationRepository conversations){this.conversations=conversations;} public UUID tenantId(UUID conversationId){return conversations.findById(conversationId).orElseThrow(()->new IllegalArgumentException("Conversation is unavailable")).tenantId();} }
