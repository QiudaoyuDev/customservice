package com.hardwareai.support.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareai.support.analytics.OperationalEventService;
import com.hardwareai.support.llm.Intent;
import com.hardwareai.support.llm.ModelConfigurationService;
import com.hardwareai.support.retrieval.RetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns evidence-gated answer generation, assistant persistence and immutable answer trace creation.
 */
@Service
class SupportOrchestratorService {
    private final RetrievalService retrieval;
    private final ConversationMessageRepository messages;
    private final AnswerTraceRepository traces;
    private final AnswerCitationRepository citations;
    private final ModelConfigurationService models;
    private final ObjectMapper json;
    private final OperationalEventService events;

    SupportOrchestratorService(RetrievalService retrieval, ConversationMessageRepository messages,
        AnswerTraceRepository traces, AnswerCitationRepository citations, ModelConfigurationService models, ObjectMapper json,
        OperationalEventService events) {
        this.retrieval = retrieval;
        this.messages = messages;
        this.traces = traces;
        this.citations = citations;
        this.models = models;
        this.json = json;
        this.events = events;
    }

    @Transactional
    Result answer(Conversation conversation, ConversationProductContext context, ConversationMessage question, Intent intent) {
        long start = System.nanoTime();
        var retrieved = retrieval.retrieve(new RetrievalService.RetrievalRequest(conversation.tenantId(), context.productModelId(),
            context.productVariantId(), conversation.region(), context.hardwareRevision(), context.firmwareVersion(),
            conversation.language(), question.content(), question.errorCode(), 3, 0.0d));
        events.record(conversation.tenantId(), conversation.id(), "RETRIEVAL_COMPLETED", java.util.Map.of(
            "outcome", retrieved.conflictDetected() ? "CONFLICT" : retrieved.evidence().isEmpty() ? "NO_EVIDENCE" : "EVIDENCE_FOUND",
            "citationCount", retrieved.evidence().size()));
        if (retrieved.conflictDetected())
            return persist(conversation, question, intent, "CONFLICT", "Evidence conflicts; please contact support.", List.of(),
                start);
        if (retrieved.evidence().isEmpty()) return persist(conversation, question, intent, "NO_EVIDENCE",
            "I do not have enough verified information. Please contact support.", List.of(), start);
        // Safe template fallback: no raw whole-document dump and no instruction not backed by selected evidence.
        String summary =
            retrieved.evidence().stream().map(e -> "[C" + (retrieved.evidence().indexOf(e) + 1) + "] " + excerpt(e.excerpt()))
                .collect(java.util.stream.Collectors.joining("\n"));
        try {
            var response = models.generate(conversation.tenantId(), systemPrompt(),
                "Question: " + question.content() + "\nEvidence:\n" + summary);
            if (response.isPresent()) {
                String answer = validateModelOutput(response.get().content(), retrieved.evidence().size());
                if (answer != null) return persist(conversation, question, intent, "MODEL", answer, retrieved.evidence(), start,
                    response.get().configurationId());
            }
        } catch (Exception ignored) {
        }
        return persist(conversation, question, intent, "EVIDENCE_TEMPLATE", summary, retrieved.evidence(), start);
    }

    private Result persist(Conversation conversation, ConversationMessage question, Intent intent, String outcome, String content,
        List<RetrievalService.Evidence> evidence, long start) {
        return persist(conversation, question, intent, outcome, content, evidence, start, null);
    }

    private Result persist(Conversation conversation, ConversationMessage question, Intent intent, String outcome, String content,
        List<RetrievalService.Evidence> evidence, long start, java.util.UUID modelConfigurationId) {
        var assistant = messages.save(ConversationMessage.assistant(conversation.id(), content));
        var trace = traces.save(new AnswerTrace(conversation.id(), assistant.id(), modelConfigurationId, intent.name(), outcome,
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), outcome, evidence.size(),
            evidence.size()));
        evidence.stream().filter(item -> item.revisionId() != null)
            .forEach(item -> citations.save(new AnswerCitation(trace.id(), item.revisionId(), item.chunkId())));
        events.record(conversation.tenantId(), conversation.id(), "ANSWER_COMPLETED",
            java.util.Map.of("intent", intent.name(), "outcome", outcome, "citationCount", evidence.size(), "latencyMs",
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
        return new Result(content, evidence.stream().map(item -> item.documentTitle() + "#" + item.chunkId()).toList(), outcome);
    }

    private static String excerpt(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 700));
    }

    @SuppressWarnings("unchecked")
    private String validateModelOutput(String value, int evidenceCount) throws java.io.IOException {
        var data = json.readValue(value, java.util.Map.class);
        Object answer = data.get("answer");
        Object rawCitations = data.get("citations");
        if (!(answer instanceof String text) || text.isBlank() || !(rawCitations instanceof List<?> keys) || keys.isEmpty())
            return null;
        for (Object key : keys)
            if (!(key instanceof String valueKey) || !valueKey.matches("C[1-" + evidenceCount + "]")) return null;
        return text;
    }

    private static String systemPrompt() {
        return "Return JSON only: {answer:string,citations:string[],followUpQuestion:string|null,resolutionCandidate:boolean,handoffRecommended:boolean}. Use only supplied Evidence, cite every factual conclusion with Cn, do not give disassembly, flashing or unsafe repair instructions.";
    }

    record Result(String content, List<String> citations, String outcome) {
    }
}
