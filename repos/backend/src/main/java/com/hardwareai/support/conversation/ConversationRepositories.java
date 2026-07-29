package com.hardwareai.support.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Conversation> findTop100ByStatusAndClosedAtBeforeOrderByClosedAtAsc(Conversation.Status status, Instant cutoff);
}

interface ConversationProductContextRepository extends JpaRepository<ConversationProductContext, UUID> {
    List<ConversationProductContext> findAllByConversationIdAndActiveTrue(UUID conversationId);
}

interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}

interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
    List<MessageAttachment> findAllByMessageIdIn(List<UUID> messageIds);

    List<MessageAttachment> findAllByMessageIdInOrderByCreatedAtAsc(List<UUID> messageIds);
}

interface ConversationFeedbackRepository extends JpaRepository<ConversationFeedback, UUID> {
}

interface AnswerTraceRepository extends JpaRepository<AnswerTrace, UUID> {
    List<AnswerTrace> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}

interface AnswerCitationRepository extends JpaRepository<AnswerCitation, UUID> {
    List<AnswerCitation> findAllByAnswerTraceIdIn(List<UUID> traceIds);
}

interface AttachmentProcessingJobRepository extends JpaRepository<AttachmentProcessingJob, UUID> {
    java.util.Optional<AttachmentProcessingJob> findFirstByStatusOrderByCreatedAt(String status);
}

interface AttachmentAnalysisRepository extends JpaRepository<AttachmentAnalysis, UUID> {
    java.util.Optional<AttachmentAnalysis> findByAttachmentId(UUID attachmentId);

    List<AttachmentAnalysis> findAllByAttachmentIdIn(List<UUID> attachmentIds);
}

interface ConversationFlowSessionRepository extends JpaRepository<ConversationFlowSession, UUID> {
    Optional<ConversationFlowSession> findFirstByConversationIdAndStatusOrderByStartedAtDesc(UUID conversationId, String status);

    List<ConversationFlowSession> findAllByConversationIdOrderByStartedAtAsc(UUID conversationId);
}

interface ConversationFlowStepRepository extends JpaRepository<ConversationFlowStep, UUID> {
    List<ConversationFlowStep> findAllByFlowSessionIdInOrderByCreatedAtAsc(List<UUID> flowSessionIds);
}
