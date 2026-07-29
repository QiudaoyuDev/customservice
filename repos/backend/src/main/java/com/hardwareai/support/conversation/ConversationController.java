package com.hardwareai.support.conversation;

import com.hardwareai.support.handoff.HandoffRepository;
import com.hardwareai.support.handoff.HandoffRequest;
import com.hardwareai.support.knowledge.EvidenceService;
import com.hardwareai.support.knowledge.ObjectStorage;
import com.hardwareai.support.retrieval.RetrievalService;
import com.hardwareai.support.config.AppProperties;
import com.hardwareai.support.llm.Intent;
import com.hardwareai.support.llm.IntentClassifier;
import com.hardwareai.support.llm.OpenAiCompatibleProvider;
import com.hardwareai.support.qr.QrApplicationService;
import com.hardwareai.support.troubleshoot.*;
import com.hardwareai.support.troubleshoot.FlowVersionService;
import com.hardwareai.support.analytics.OperationalEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Public customer API. The QR token is the only authority for initial product scope.
 */
@RestController
@RequestMapping("/public/conversations")
class ConversationController {
    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);
    private static final Set<UUID> ACTIVE_STREAMS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final QrApplicationService qr;
    private final ConversationRepository conversations;
    private final ConversationProductContextRepository contexts;
    private final ConversationContextService contextService;
    private final ConversationMessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final ConversationFeedbackRepository feedback;
    private final ObjectStorage storage;
    private final IntentClassifier intents;
    private final RetrievalService retrieval;
    private final TroubleshootFlowRepository flows;
    private final TroubleshootNodeRepository nodes;
    private final TroubleshootStateMachine machine;
    private final HandoffRepository handoffs;
    private final MessageSource messageSource;
    private final SupportOrchestratorService orchestrator;
    private final AttachmentProcessingJobRepository attachmentJobs;
    private final AttachmentAnalysisRepository attachmentAnalyses;
    private final ConversationFlowSessionRepository flowSessions;
    private final ConversationFlowStepRepository flowSteps;
    private final FlowVersionService flowVersions;
    private final FlowMatcher flowMatcher;
    private final OperationalEventService events;

    ConversationController(QrApplicationService qr, ConversationRepository conversations, ConversationProductContextRepository contexts, ConversationContextService contextService, ConversationMessageRepository messages, MessageAttachmentRepository attachments, ConversationFeedbackRepository feedback, ObjectStorage storage, IntentClassifier intents, RetrievalService retrieval, TroubleshootFlowRepository flows, TroubleshootNodeRepository nodes, TroubleshootStateMachine machine, HandoffRepository handoffs, MessageSource messageSource, SupportOrchestratorService orchestrator, AttachmentProcessingJobRepository attachmentJobs, AttachmentAnalysisRepository attachmentAnalyses, ConversationFlowSessionRepository flowSessions, ConversationFlowStepRepository flowSteps, FlowVersionService flowVersions, FlowMatcher flowMatcher, OperationalEventService events) {
        this.qr = qr;
        this.conversations = conversations;
        this.contexts = contexts;
        this.contextService = contextService;
        this.messages = messages;
        this.attachments = attachments;
        this.feedback = feedback;
        this.storage = storage;
        this.intents = intents;
        this.retrieval = retrieval;
        this.flows = flows;
        this.nodes = nodes;
        this.machine = machine;
        this.handoffs = handoffs;
        this.messageSource = messageSource;
        this.orchestrator = orchestrator;
        this.attachmentJobs = attachmentJobs;
        this.attachmentAnalyses = attachmentAnalyses;
        this.flowSessions = flowSessions;
        this.flowSteps = flowSteps;
        this.flowVersions = flowVersions;
        this.flowMatcher = flowMatcher;
        this.events = events;
    }

    /**
     * 将会话语言代码（如 en-US / zh-CN）映射为 Locale。
     */
    private Locale toLocale(String language) {
        if (language == null) return Locale.ENGLISH;
        return switch (language.toLowerCase(Locale.ROOT).split("[-_]", 2)[0]) {
            case "zh" -> Locale.CHINESE;
            case "en" -> Locale.ENGLISH;
            default -> Locale.ENGLISH;
        };
    }

    /**
     * 按会话语言取本地化消息，支持 {0} 占位符。
     */
    private String msg(String language, String code, Object... args) {
        return messageSource.getMessage(code, args, toLocale(language));
    }

    @PostMapping
    public View create(@Valid @RequestBody Create request) {
        var resolved = qr.resolve(request.qrToken());
        var binding = resolved.binding();
        var product = resolved.product();
        String accessToken = UUID.randomUUID() + "." + UUID.randomUUID();
        var conversation = conversations.save(new Conversation(binding.tenantId(), binding.id(), request.language(), product.region(), hash(accessToken)));
        contextService.establishFromQr(conversation, resolved, request.hardwareVersion(), request.firmwareVersion());
        events.record(conversation.tenantId(), conversation.id(), "CONVERSATION_CREATED", Map.of("status", conversation.status().name()));
        log.info("Conversation created id={} tenant={} product={} language={} region={}", conversation.id(), binding.tenantId(), binding.productModelId(), request.language(), product.region());
        return new View(conversation.id(), binding.productModelId(), request.language(), product.region(), accessToken);
    }

    @PostMapping("/{id}/messages")
    public MessageView send(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken, @Valid @RequestBody Send request) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        var message = messages.save(new ConversationMessage(conversation.id(), request.content(), request.errorCode(), request.controlledReply()));
        events.record(conversation.tenantId(), conversation.id(), "MESSAGE_RECEIVED", Map.of("errorCodePresent", request.errorCode() != null));
        log.debug("Message received conversation={} errorCode={} contentLen={}", id, request.errorCode(), request.content().length());
        return MessageView.of(message);
    }

    @PostMapping("/{id}/product-context")
    public void changeProduct(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken, @Valid @RequestBody ChangeProduct request) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        contextService.replaceByUser(conversation, request.productModelId(), request.productVariantId(), request.hardwareRevision(), request.firmwareVersion());
        conversations.save(conversation);
    }

    @GetMapping("/{id}/product-options")
    public List<ProductOption> productOptions(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        return contextService.selectableProducts(conversation).stream()
                .map(product -> new ProductOption(product.id(), product.displayName(), product.model(), product.region(), product.hardwareVersion()))
                .toList();
    }

    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    public AttachmentView uploadAttachment(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken, @RequestPart @NotBlank @Size(max = 4000) String content, @RequestPart(required = false) @Size(max = 100) String errorCode, @RequestPart MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 || !Set.of("image/png", "image/jpeg").contains(file.getContentType()) || !validImageSignature(file))
            throw new IllegalArgumentException("Only PNG or JPEG up to 10 MiB is supported");
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        var message = messages.save(new ConversationMessage(conversation.id(), content, errorCode));
        String key = conversation.tenantId() + "/conversations/" + id + "/" + UUID.randomUUID();
        storage.put(key, file);
        var attachment = attachments.save(new MessageAttachment(message.id(), key, file.getContentType(), file.getSize()));
        attachmentJobs.save(new AttachmentProcessingJob(attachment.id()));
        events.record(conversation.tenantId(), conversation.id(), "ATTACHMENT_UPLOADED", Map.of("attachmentType", file.getContentType()));
        return new AttachmentView(message.id(), attachment.id());
    }

    @GetMapping("/{id}/attachments/{attachmentId}/analysis")
    public AttachmentAnalysisView attachmentAnalysis(@PathVariable UUID id, @PathVariable UUID attachmentId, @RequestHeader("X-Conversation-Token") String accessToken) {
        authorizeAttachment(id, attachmentId, accessToken);
        return attachmentAnalyses.findByAttachmentId(attachmentId).map(AttachmentAnalysisView::of).orElseThrow(() -> new IllegalStateException("Attachment analysis is pending"));
    }

    @PostMapping("/{id}/attachments/{attachmentId}/analysis/confirm")
    public AttachmentAnalysisView confirmAttachmentAnalysis(@PathVariable UUID id, @PathVariable UUID attachmentId, @RequestHeader("X-Conversation-Token") String accessToken) {
        authorizeAttachment(id, attachmentId, accessToken);
        var analysis = attachmentAnalyses.findByAttachmentId(attachmentId).orElseThrow(() -> new IllegalStateException("Attachment analysis is pending"));
        analysis.confirm(); attachmentAnalyses.save(analysis);
        return AttachmentAnalysisView.of(analysis);
    }

    @PostMapping("/{id}/feedback")
    public void submitFeedback(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken, @Valid @RequestBody Feedback request) {
        var conversation = conversations.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        feedback.save(new ConversationFeedback(id, request.resolved(), request.comment()));
    }

    @GetMapping("/{id}/messages")
    public List<MessageView> history(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken) {
        var conversation = conversations.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        return messages.findAllByConversationIdOrderByCreatedAtAsc(id).stream().map(MessageView::of).toList();
    }

    @PostMapping("/{id}/answers")
    public Answer answer(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        var history = messages.findAllByConversationIdOrderByCreatedAtAsc(id);
        if (history.isEmpty()) throw new IllegalArgumentException("A user message is required");
        Intent intent = intents.classify(history.getLast().content());
        log.info("Answer requested conversation={} classifiedIntent={}", id, intent);
        if (intent == Intent.SAFETY_RISK) {
            log.warn("Safety risk detected conversation={} -> stopping self-service", id);
            return new Answer(intent, msg(conversation.language(), "answer.safety"), List.of(), null, null, null);
        }
        if (intent == Intent.HUMAN_REQUEST)
            return new Answer(intent, msg(conversation.language(), "answer.human"), List.of(), null, null, null);
        if (intent == Intent.UNKNOWN)
            return new Answer(intent, msg(conversation.language(), "answer.noKnowledge"), List.of(), null, null, null);
        var context = contexts.findAllByConversationIdAndActiveTrue(id).stream().findFirst().orElseThrow(() -> new IllegalStateException("Product context is unavailable"));
        if (intent == Intent.TROUBLESHOOTING || intent == Intent.ERROR_CODE) {
            var flow = flowMatcher.match(conversation.tenantId(), new FlowMatchScope(context.productModelId(), context.productVariantId(), context.hardwareRevision(), context.firmwareVersion()),
                    conversation.region(), conversation.language(), intent, history.getLast().content(), history.getLast().errorCode());
            if (flow.isPresent()) {
                log.info("Matched troubleshoot flow conversation={} flow={} intent={}", id, flow.get().id(), intent);
                return driveFlow(conversation, history, flow.get());
            }
        }
        var result = orchestrator.answer(conversation, context, history.getLast(), intent);
        log.info("Evidence answer conversation={} intent={} citations={} outcome={}", id, intent, result.citations().size(), result.outcome());
        return new Answer(intent, result.content(), result.citations(), null, null, null);
    }

    @GetMapping(value = "/{id}/answers/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable UUID id, @RequestHeader("X-Conversation-Token") String accessToken) {
        var emitter = new SseEmitter(30_000L);
        if (!ACTIVE_STREAMS.add(id)) { emitter.completeWithError(new IllegalStateException("An answer is already being generated")); return emitter; }
        var cancelled = new AtomicBoolean(false);
        var completed = new AtomicBoolean(false);
        var task = new AtomicReference<Future<?>>();
        Runnable cancel = () -> {
            ACTIVE_STREAMS.remove(id);
            if (!completed.get()) {
                cancelled.set(true);
                var running = task.get();
                if (running != null) running.cancel(true);
            }
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        task.set(STREAM_EXECUTOR.submit(() -> {
            try {
                var result = answer(id, accessToken);
                if (cancelled.get()) return;
                String answerId = UUID.randomUUID().toString();
                emitter.send(SseEmitter.event().name("meta").data(Map.of("answerId", answerId, "intent", result.intent().name())));
                for (String chunk : streamChunks(result.content(), 160)) {
                    if (cancelled.get() || Thread.currentThread().isInterrupted()) return;
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("answerId", answerId, "content", chunk)));
                }
                if (cancelled.get()) return;
                emitter.send(SseEmitter.event().name("citations").data(result.citations()));
                emitter.send(SseEmitter.event().name("done").data(Map.of("intent", result.intent().name())));
                completed.set(true);
                emitter.complete();
            } catch (Exception exception) {
                if (!cancelled.get()) {
                    try { emitter.send(SseEmitter.event().name("error").data(Map.of("code", "ANSWER_FAILED"))); } catch (Exception ignored) { }
                    completed.set(true);
                    emitter.completeWithError(exception);
                }
            } finally { ACTIVE_STREAMS.remove(id); }
        }));
        return emitter;
    }

    static List<String> streamChunks(String content, int maxChars) {
        if (content == null || content.isEmpty()) return List.of("");
        var chunks = new ArrayList<String>();
        for (int start = 0; start < content.length(); start += maxChars)
            chunks.add(content.substring(start, Math.min(content.length(), start + maxChars)));
        return chunks;
    }

    private Answer driveFlow(Conversation conversation, List<ConversationMessage> history, TroubleshootFlow matchedFlow) {
        var active = flowSessions.findFirstByConversationIdAndStatusOrderByStartedAtDesc(conversation.id(), "ACTIVE");
        if (active.isPresent()) {
            var definition = flowVersions.byId(active.get().flowVersionId());
            var pinnedFlow = flows.findById(definition.flowId())
                    .filter(flow -> flow.tenantId().equals(conversation.tenantId()))
                    .orElseThrow(() -> new IllegalStateException("Pinned flow is unavailable"));
            return advanceFlow(conversation, history, pinnedFlow, active.get(), definition);
        }
        var definition = flowVersions.latest(matchedFlow.id());
        var start = definition.start();
        var session = flowSessions.save(new ConversationFlowSession(conversation.id(), definition.snapshotId(), start.nodeKey()));
        conversation.startFlow(matchedFlow.id());
        conversation.setNode(start.nodeKey(), 0);
        conversations.save(conversation);
        log.info("Flow started conversation={} flow={} version={} firstNode={}", conversation.id(), matchedFlow.id(), definition.snapshotId(), start.nodeKey());
        events.record(conversation.tenantId(), conversation.id(), "FLOW_STARTED", Map.of("flowVersion", definition.snapshotId().toString(), "nodeType", start.nodeType()));
        return renderFlowNode(conversation, matchedFlow, session, definition, start);
    }

    private Answer advanceFlow(Conversation conversation, List<ConversationMessage> history, TroubleshootFlow flow,
                               ConversationFlowSession session, FlowVersionService.Definition definition) {
        var byKey = definition.nodes().stream().collect(Collectors.toMap(FlowVersionService.Node::nodeKey, node -> node));
        var current = byKey.get(session.currentNodeKey());
        if (current == null) throw new IllegalStateException("Pinned flow node is unavailable");
        var reply = normalizeReply(history.getLast(), current);
        int failures = session.failureCount();
        if ((nodeType(current) == TroubleshootTypes.NodeType.OPERATION && reply == TroubleshootTypes.Reply.NO)
                || reply == TroubleshootTypes.Reply.UNKNOWN) failures++;
        String yes = nodeType(current) == TroubleshootTypes.NodeType.OPERATION ? current.branchNext() : current.branchYes();
        String no = nodeType(current) == TroubleshootTypes.NodeType.OPERATION ? current.branchNext() : current.branchNo();
        var transition = machine.next(nodeType(current), risk(current), reply, yes, no, current.branchUnknown(), failures);
        String result = transition.escalated() ? "ESCALATED" : transition.nextNodeKey() == null ? "COMPLETED" : "NEXT:" + transition.nextNodeKey();
        flowSteps.save(new ConversationFlowStep(session.id(), current.nodeKey(), reply.name(), history.getLast().id(), result));
        events.record(conversation.tenantId(), conversation.id(), "FLOW_STEP", Map.of("flowVersion", definition.snapshotId().toString(), "nodeType", current.nodeType(), "outcome", result));
        log.info("Flow step conversation={} flow={} version={} fromNode={} reply={} failures={} -> nextNode={} escalated={}",
                conversation.id(), flow.id(), definition.snapshotId(), current.nodeKey(), reply, failures, transition.nextNodeKey(), transition.escalated());
        if (transition.escalated()) {
            session.complete();
            flowSessions.save(session);
            return escalate(conversation, flow);
        }
        var next = byKey.get(transition.nextNodeKey());
        if (next == null) {
            session.complete();
            flowSessions.save(session);
            conversation.clearFlow();
            conversations.save(conversation);
            log.info("Flow ended conversation={} flow={} version={} lastNode={}", conversation.id(), flow.id(), definition.snapshotId(), current.nodeKey());
            return new Answer(Intent.TROUBLESHOOTING, msg(conversation.language(), "flow.end"), List.of(), null, null, null);
        }
        session.advance(next.nodeKey(), failures);
        flowSessions.save(session);
        conversation.setNode(next.nodeKey(), failures);
        conversations.save(conversation);
        return renderFlowNode(conversation, flow, session, definition, next);
    }

    private Answer renderFlowNode(Conversation conversation, TroubleshootFlow flow, ConversationFlowSession session,
                                  FlowVersionService.Definition definition, FlowVersionService.Node node) {
        if (nodeType(node) == TroubleshootTypes.NodeType.HUMAN_ESCALATION || node.safetyStop() || risk(node) == TroubleshootTypes.Risk.HIGH) {
            session.complete();
            flowSessions.save(session);
            return escalate(conversation, flow);
        }
        boolean end = nodeType(node) == TroubleshootTypes.NodeType.END;
        if (end) {
            session.complete();
            flowSessions.save(session);
            conversation.clearFlow();
            conversations.save(conversation);
        }
        var byKey = definition.nodes().stream().collect(Collectors.toMap(FlowVersionService.Node::nodeKey, value -> value));
        var path = pathTo(definition.nodes(), byKey, node.nodeKey());
        var refs = node.sourceRefs() == null ? List.<String>of() : node.sourceRefs();
        var control = new FlowControl(flow.id(), node.nodeKey(), node.nodeType(), node.expectedInput(), node.risk(), path, definition.nodes().size(), end, false);
        return new Answer(Intent.TROUBLESHOOTING, node.prompt(), new ArrayList<>(refs), node.expectedInput(), node.risk(), control);
    }

    private TroubleshootTypes.NodeType nodeType(FlowVersionService.Node node) {
        return TroubleshootTypes.NodeType.valueOf(node.nodeType());
    }

    private TroubleshootTypes.Risk risk(FlowVersionService.Node node) {
        return TroubleshootTypes.Risk.valueOf(node.risk());
    }

    private TroubleshootTypes.Reply normalizeReply(ConversationMessage message, FlowVersionService.Node node) {
        if (message.controlledReply() != null) return message.controlledReply();
        var v = message.content().toLowerCase(Locale.ROOT);
        if ("free_text".equals(node.expectedInput())) {
            if (contains(v, "未解决", "没好", "失败", "无效", "不行", "no", "fail", "not", "无效"))
                return TroubleshootTypes.Reply.NO;
            return TroubleshootTypes.Reply.YES;
        }
        if (contains(v, "是", "能", "可以", "yes", "can", "resolved", "解决", "ok")) return TroubleshootTypes.Reply.YES;
        if (contains(v, "否", "不能", "无法", "no", "cannot")) return TroubleshootTypes.Reply.NO;
        if (contains(v, "不清楚", "不知道", "不确定", "unknown")) return TroubleshootTypes.Reply.UNKNOWN;
        if (contains(v, "拒绝", "refuse")) return TroubleshootTypes.Reply.REFUSE;
        return TroubleshootTypes.Reply.UNKNOWN;
    }

    private List<String> pathTo(List<FlowVersionService.Node> list, Map<String, FlowVersionService.Node> byKey, String target) {
        var path = new ArrayList<String>();
        var seen = new HashSet<String>();
        var cur = list.getFirst();
        while (cur != null && !seen.contains(cur.nodeKey()) && path.size() <= 100) {
            path.add(cur.nodeKey());
            seen.add(cur.nodeKey());
            if (cur.nodeKey().equals(target)) break;
            String next = null;
            if (cur.branchYes() != null) next = cur.branchYes();
            else if (cur.branchNext() != null) next = cur.branchNext();
            else if (cur.branchNo() != null) next = cur.branchNo();
            else if (cur.branchUnknown() != null) next = cur.branchUnknown();
            cur = next == null ? null : byKey.get(next);
        }
        return path;
    }

    private boolean contains(String value, String... words) {
        for (String w : words) if (value.contains(w)) return true;
        return false;
    }

    private Answer escalate(Conversation conversation, TroubleshootFlow flow) {
        var handoff = handoffs.save(new HandoffRequest(conversation.tenantId(), conversation.id(), "flow-" + UUID.randomUUID(),
                msg(conversation.language(), "handoff.title", flow.title()), msg(conversation.language(), "handoff.desc"), true));
        conversation.clearFlow();
        conversations.save(conversation);
        log.warn("Flow escalated to human conversation={} flow={} handoff={}", conversation.id(), flow.id(), handoff.id());
        return new Answer(Intent.HUMAN_REQUEST, msg(conversation.language(), "flow.escalated", handoff.id()), List.of(), null, null, null);
    }

    private void authorize(Conversation conversation, String accessToken) {
        if (accessToken == null || !conversation.authorizes(hash(accessToken)))
            throw new IllegalArgumentException("Conversation access is invalid");
    }

    private void authorizeAttachment(UUID conversationId, UUID attachmentId, String accessToken) {
        var conversation = conversations.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        authorize(conversation, accessToken);
        var attachment = attachments.findById(attachmentId).orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        var message = messages.findById(attachment.messageId()).orElseThrow(() -> new IllegalArgumentException("Attachment message not found"));
        if (!conversationId.equals(message.conversationId())) throw new IllegalArgumentException("Attachment is not in this conversation");
    }

    private boolean validImageSignature(MultipartFile file) {
        try {
            byte[] bytes = file.getInputStream().readNBytes(12);
            boolean png = bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
            boolean jpeg = bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
            return "image/png".equals(file.getContentType()) ? png : jpeg;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    record Create(@NotBlank String qrToken, @NotBlank @Size(max = 16) String language,
                  @Size(max = 80) String hardwareVersion, @Size(max = 80) String firmwareVersion) {
    }

    record Send(@NotBlank @Size(max = 4000) String content, @Size(max = 100) String errorCode,
                TroubleshootTypes.Reply controlledReply) {
    }

    record ChangeProduct(@NotNull UUID productModelId, UUID productVariantId, @Size(max = 80) String hardwareRevision,
                         @Size(max = 80) String firmwareVersion) {
    }

    record ProductOption(UUID id, String displayName, String model, String region, String hardwareVersion) { }

    record Feedback(boolean resolved, @Size(max = 1000) String comment) {
    }

    record AttachmentView(UUID messageId, UUID attachmentId) {
    }
    record AttachmentAnalysisView(String ocrText, String errorCode, Double confidence, boolean requiresConfirmation, String status) {
        static AttachmentAnalysisView of(AttachmentAnalysis value) { return new AttachmentAnalysisView(value.ocrText(), value.errorCode(), value.confidence(), value.requiresConfirmation(), value.status()); }
    }

    record Answer(Intent intent, String content, List<String> citations, String expectedInput, String risk,
                  FlowControl flowControl) {
    }

    record FlowControl(UUID flowId, String nodeKey, String nodeType, String expectedInput, String risk,
                       List<String> path, int totalSteps, boolean end, boolean escalated) {
    }

    record View(UUID id, UUID productModelId, String language, String region, String conversationAccessToken) {
    }

    record MessageView(UUID id, String sender, String content, String errorCode, Instant createdAt) {
        static MessageView of(ConversationMessage message) {
            return new MessageView(message.id(), message.sender().name(), message.content(), message.errorCode(), message.createdAt());
        }
    }
}
