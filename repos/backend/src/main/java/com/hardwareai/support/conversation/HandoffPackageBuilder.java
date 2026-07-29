package com.hardwareai.support.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds an immutable, server-owned handoff package without logging customer content or file bodies.
 */
@Service
public class HandoffPackageBuilder {
    private final ConversationRepository conversations;
    private final ConversationProductContextRepository contexts;
    private final ConversationMessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final AttachmentAnalysisRepository analyses;
    private final AnswerTraceRepository traces;
    private final AnswerCitationRepository citations;
    private final ConversationFlowSessionRepository sessions;
    private final ConversationFlowStepRepository steps;
    private final ObjectMapper json;

    HandoffPackageBuilder(ConversationRepository conversations, ConversationProductContextRepository contexts,
        ConversationMessageRepository messages, MessageAttachmentRepository attachments,
        AttachmentAnalysisRepository analyses, AnswerTraceRepository traces,
        AnswerCitationRepository citations, ConversationFlowSessionRepository sessions,
        ConversationFlowStepRepository steps, ObjectMapper json) {
        this.conversations = conversations;
        this.contexts = contexts;
        this.messages = messages;
        this.attachments = attachments;
        this.analyses = analyses;
        this.traces = traces;
        this.citations = citations;
        this.sessions = sessions;
        this.steps = steps;
        this.json = json;
    }

    public String build(UUID tenantId, UUID conversationId, String reason, String userSupplement, String contact,
        boolean contactAuthorized) {
        var conversation = conversations.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        var history = messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        var messageIds = history.stream().map(ConversationMessage::id).toList();
        var files = messageIds.isEmpty() ? List.<MessageAttachment>of() : attachments.findAllByMessageIdIn(messageIds);
        var attachmentIds = files.stream().map(MessageAttachment::id).toList();
        var analysisByAttachment =
            (attachmentIds.isEmpty() ? List.<AttachmentAnalysis>of() : analyses.findAllByAttachmentIdIn(attachmentIds)).stream()
                .collect(Collectors.toMap(AttachmentAnalysis::attachmentId, analysis -> analysis));
        var answerTraces = traces.findAllByConversationIdOrderByCreatedAtAsc(conversationId);
        var traceIds = answerTraces.stream().map(AnswerTrace::id).toList();
        var citationsByTrace =
            (traceIds.isEmpty() ? List.<AnswerCitation>of() : citations.findAllByAnswerTraceIdIn(traceIds)).stream()
                .collect(Collectors.groupingBy(AnswerCitation::answerTraceId));
        var flowSessions = sessions.findAllByConversationIdOrderByStartedAtAsc(conversationId);
        var sessionIds = flowSessions.stream().map(ConversationFlowSession::id).toList();
        var flowSteps =
            sessionIds.isEmpty() ? List.<ConversationFlowStep>of() : steps.findAllByFlowSessionIdInOrderByCreatedAtAsc(sessionIds);

        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("schemaVersion", 2);
        snapshot.put("generatedAt", Instant.now());
        snapshot.put("requestId", MDC.get("requestId"));
        snapshot.put("conversationId", conversation.id());
        snapshot.put("tenantId", conversation.tenantId());
        snapshot.put("region", conversation.region());
        snapshot.put("language", conversation.language());
        snapshot.put("reason", reason);
        snapshot.put("userSupplement", userSupplement);
        snapshot.put("contact", contactAuthorized ? contact : null);
        snapshot.put("contactAuthorized", contactAuthorized);
        snapshot.put("channel", "web");
        snapshot.put("productContexts", contexts.findAllByConversationIdAndActiveTrue(conversationId).stream()
            .map(context -> Map.of("productModelId", context.productModelId(), "productVariantId", empty(context.productVariantId()),
                "hardwareRevision", empty(context.hardwareRevision()), "firmwareVersion", empty(context.firmwareVersion())))
            .toList());
        snapshot.put("messages", history.stream().map(
            message -> Map.of("id", message.id(), "sender", message.sender().name(), "content", message.content(), "errorCode",
                empty(message.errorCode()), "createdAt", message.createdAt().toString())).toList());
        snapshot.put("attachments", files.stream().map(file -> {
            var analysis = analysisByAttachment.get(file.id());
            var item = new LinkedHashMap<String, Object>();
            item.put("attachmentId", file.id());
            item.put("messageId", file.messageId());
            item.put("objectKey", file.objectKey());
            item.put("contentType", file.contentType());
            item.put("sizeBytes", file.sizeBytes());
            item.put("createdAt", file.createdAt());
            if (analysis != null) item.put("analysis",
                linked("ocrText", empty(analysis.ocrText()), "errorCode", empty(analysis.errorCode()), "confidence",
                    analysis.confidence(), "requiresConfirmation", analysis.requiresConfirmation(), "status", analysis.status()));
            return item;
        }).toList());
        snapshot.put("answerTraces", answerTraces.stream().map(
            trace -> linked("traceId", trace.id(), "messageId", trace.messageId(), "intent", trace.intent(), "outcome",
                trace.outcome(), "latencyMs", trace.latencyMs(), "promptVersion", trace.promptVersion(), "finishReason",
                trace.finishReason(), "retrievalCount", trace.retrievalCount(), "selectedEvidenceCount",
                trace.selectedEvidenceCount(), "citations", citationsByTrace.getOrDefault(trace.id(), List.of()).stream()
                    .map(citation -> Map.of("revisionId", citation.revisionId(), "chunkId", citation.chunkId())).toList(),
                "createdAt", trace.createdAt())).toList());
        snapshot.put("flowSessions", flowSessions.stream().map(
            session -> linked("sessionId", session.id(), "flowVersionId", session.flowVersionId(), "currentNodeKey",
                empty(session.currentNodeKey()), "failureCount", session.failureCount(), "status", session.status(), "startedAt",
                session.startedAt(), "endedAt", session.endedAt())).toList());
        snapshot.put("flowSteps", flowSteps.stream().map(
                step -> Map.of("flowSessionId", step.flowSessionId(), "nodeKey", step.nodeKey(), "normalizedReply",
                    step.normalizedReply(), "rawMessageId", step.rawMessageId(), "result", step.result(), "createdAt", step.createdAt()))
            .toList());
        try {
            return json.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build handoff package", exception);
        }
    }

    private static String empty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Map<String, Object> linked(Object... values) {
        var result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < values.length; index += 2) result.put((String)values[index], values[index + 1]);
        return result;
    }
}
