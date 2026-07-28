package com.hardwareai.support.conversation;

import com.hardwareai.support.handoff.HandoffRepository;
import com.hardwareai.support.handoff.HandoffRequest;
import com.hardwareai.support.knowledge.EvidenceService;
import com.hardwareai.support.knowledge.ObjectStorage;
import com.hardwareai.support.llm.Intent;
import com.hardwareai.support.llm.IntentClassifier;
import com.hardwareai.support.product.ProductRepository;
import com.hardwareai.support.qr.QrBinding;
import com.hardwareai.support.qr.QrBindingRepository;
import com.hardwareai.support.troubleshoot.*;
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
import java.util.stream.Collectors;

/**
 * Public customer API. The QR token is the only authority for initial product scope.
 */
@RestController
@RequestMapping("/public/conversations")
class ConversationController {
    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);
    private final QrBindingRepository bindings;
    private final ProductRepository products;
    private final ConversationRepository conversations;
    private final ConversationProductContextRepository contexts;
    private final ConversationMessageRepository messages;
    private final MessageAttachmentRepository attachments;
    private final ConversationFeedbackRepository feedback;
    private final ObjectStorage storage;
    private final IntentClassifier intents;
    private final EvidenceService evidence;
    private final TroubleshootFlowRepository flows;
    private final TroubleshootNodeRepository nodes;
    private final TroubleshootStateMachine machine;
    private final HandoffRepository handoffs;
    private final MessageSource messageSource;

    ConversationController(QrBindingRepository bindings, ProductRepository products, ConversationRepository conversations, ConversationProductContextRepository contexts, ConversationMessageRepository messages, MessageAttachmentRepository attachments, ConversationFeedbackRepository feedback, ObjectStorage storage, IntentClassifier intents, EvidenceService evidence, TroubleshootFlowRepository flows, TroubleshootNodeRepository nodes, TroubleshootStateMachine machine, HandoffRepository handoffs, MessageSource messageSource) {
        this.bindings = bindings;
        this.products = products;
        this.conversations = conversations;
        this.contexts = contexts;
        this.messages = messages;
        this.attachments = attachments;
        this.feedback = feedback;
        this.storage = storage;
        this.intents = intents;
        this.evidence = evidence;
        this.flows = flows;
        this.nodes = nodes;
        this.machine = machine;
        this.handoffs = handoffs;
        this.messageSource = messageSource;
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
        var binding = bindings.findByTokenHash(hash(request.qrToken())).filter(QrBinding::valid).orElseThrow(() -> new IllegalArgumentException("QR token is invalid, revoked, or expired"));
        var product = products.findById(binding.productModelId()).orElseThrow(() -> new IllegalArgumentException("Product is unavailable"));
        var conversation = conversations.save(new Conversation(binding.tenantId(), binding.id(), request.language(), product.region()));
        contexts.save(new ConversationProductContext(conversation.id(), binding.productModelId(), request.hardwareVersion(), request.firmwareVersion(), "QR"));
        log.info("Conversation created id={} tenant={} product={} language={} region={}", conversation.id(), binding.tenantId(), binding.productModelId(), request.language(), product.region());
        return new View(conversation.id(), binding.productModelId(), request.language(), product.region());
    }

    @PostMapping("/{id}/messages")
    public MessageView send(@PathVariable UUID id, @Valid @RequestBody Send request) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        var message = messages.save(new ConversationMessage(conversation.id(), request.content(), request.errorCode()));
        log.debug("Message received conversation={} errorCode={} contentLen={}", id, request.errorCode(), request.content().length());
        return MessageView.of(message);
    }

    @PostMapping("/{id}/product-context")
    public void changeProduct(@PathVariable UUID id, @Valid @RequestBody ChangeProduct request) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        products.findByIdAndTenantId(request.productModelId(), conversation.tenantId()).orElseThrow(() -> new IllegalArgumentException("Product is unavailable"));
        contexts.findAllByConversationIdAndActiveTrue(id).forEach(context -> {
            context.close();
            contexts.save(context);
        });
        contexts.save(new ConversationProductContext(id, request.productModelId(), request.hardwareVersion(), request.firmwareVersion(), "USER_SELECTED"));
        conversation.clearFlow();
        conversations.save(conversation);
    }

    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    public AttachmentView uploadAttachment(@PathVariable UUID id, @RequestPart @NotBlank @Size(max = 4000) String content, @RequestPart(required = false) @Size(max = 100) String errorCode, @RequestPart MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 || !Set.of("image/png", "image/jpeg").contains(file.getContentType()))
            throw new IllegalArgumentException("Only PNG or JPEG up to 10 MiB is supported");
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        var message = messages.save(new ConversationMessage(conversation.id(), content, errorCode));
        String key = conversation.tenantId() + "/conversations/" + id + "/" + UUID.randomUUID();
        storage.put(key, file);
        var attachment = attachments.save(new MessageAttachment(message.id(), key, file.getContentType(), file.getSize()));
        return new AttachmentView(message.id(), attachment.id());
    }

    @PostMapping("/{id}/feedback")
    public void submitFeedback(@PathVariable UUID id, @Valid @RequestBody Feedback request) {
        conversations.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
        feedback.save(new ConversationFeedback(id, request.resolved(), request.comment()));
    }

    @GetMapping("/{id}/messages")
    public List<MessageView> history(@PathVariable UUID id) {
        return messages.findAllByConversationIdOrderByCreatedAtAsc(id).stream().map(MessageView::of).toList();
    }

    @PostMapping("/{id}/answers")
    public Answer answer(@PathVariable UUID id) {
        var conversation = conversations.findById(id).filter(c -> c.status() == Conversation.Status.OPEN).orElseThrow(() -> new IllegalArgumentException("Conversation is unavailable"));
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
        var context = contexts.findAllByConversationIdAndActiveTrue(id).stream().findFirst().orElseThrow(() -> new IllegalStateException("Product context is unavailable"));
        if (intent == Intent.TROUBLESHOOTING || intent == Intent.ERROR_CODE) {
            var flow = flows.findPublishedMatch(conversation.tenantId(), context.productModelId(), conversation.region(), conversation.language(), intent);
            if (flow.isPresent()) {
                log.info("Matched troubleshoot flow conversation={} flow={} intent={}", id, flow.get().id(), intent);
                return driveFlow(conversation, history, flow.get());
            }
        }
        var citations = evidence.find(conversation.tenantId(), context.productModelId(), conversation.region(), conversation.language(), history.getLast().content());
        if (citations.isEmpty()) {
            log.info("No evidence found conversation={} intent={} -> suggesting human/error-code", id, intent);
            return new Answer(intent, msg(conversation.language(), "answer.noKnowledge"), List.of(), null, null, null);
        }
        log.info("Evidence answer conversation={} intent={} citations={}", id, intent, citations.size());
        return new Answer(intent, citations.getFirst().text(), citations.stream().map(c -> c.chunkId().toString()).toList(), null, null, null);
    }

    @GetMapping(value = "/{id}/answers/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable UUID id) {
        var emitter = new SseEmitter(30_000L);
        try {
            var answer = answer(id);
            emitter.send(SseEmitter.event().name("answer").data(answer));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private Answer driveFlow(Conversation conversation, List<ConversationMessage> history, TroubleshootFlow flow) {
        var nodeList = nodes.findAllByFlowIdOrderByOrderIndexAsc(flow.id());
        if (nodeList.isEmpty())
            return new Answer(Intent.TROUBLESHOOTING, msg(conversation.language(), "flow.empty"), List.of(), null, null, null);
        var byKey = nodeList.stream().collect(Collectors.toMap(TroubleshootNode::nodeKey, n -> n));
        TroubleshootNode node;
        String nodeKey = conversation.currentNodeKey();
        if (nodeKey == null || !byKey.containsKey(nodeKey)) {
            node = nodeList.get(0);
            conversation.startFlow(flow.id());
            conversations.save(conversation);
            log.info("Flow started conversation={} flow={} firstNode={}", conversation.id(), flow.id(), node.nodeKey());
        } else {
            var current = byKey.get(nodeKey);
            var reply = normalizeReply(history.getLast().content(), current);
            int failures = conversation.flowFailures();
            if (current.nodeType() == TroubleshootTypes.NodeType.OPERATION && reply == TroubleshootTypes.Reply.NO)
                failures++;
            String yes = current.nodeType() == TroubleshootTypes.NodeType.OPERATION ? current.branchNext() : current.branchYes();
            String no = current.nodeType() == TroubleshootTypes.NodeType.OPERATION ? current.branchNext() : current.branchNo();
            var t = machine.next(current.nodeType(), current.risk(), reply, yes, no, current.branchUnknown(), failures);
            log.info("Flow step conversation={} flow={} fromNode={} reply={} failures={} -> nextNode={} escalated={}",
                    conversation.id(), flow.id(), current.nodeKey(), reply, failures, t.nextNodeKey(), t.escalated());
            if (t.escalated()) {
                var handoff = handoffs.save(new HandoffRequest(conversation.tenantId(), conversation.id(), "flow-" + UUID.randomUUID(), msg(conversation.language(), "handoff.title", flow.title()), msg(conversation.language(), "handoff.desc"), true));
                conversation.clearFlow();
                conversations.save(conversation);
                log.warn("Flow escalated to human conversation={} flow={} handoff={}", conversation.id(), flow.id(), handoff.id());
                return new Answer(Intent.HUMAN_REQUEST, msg(conversation.language(), "flow.escalated", handoff.id()), List.of(), null, null, null);
            }
            node = byKey.get(t.nextNodeKey());
            if (node == null) {
                conversation.clearFlow();
                conversations.save(conversation);
                log.info("Flow ended conversation={} flow={} lastNode={}", conversation.id(), flow.id(), current.nodeKey());
                return new Answer(Intent.TROUBLESHOOTING, msg(conversation.language(), "flow.end"), List.of(), null, null, null);
            }
            conversation.setNode(node.nodeKey(), failures);
            conversations.save(conversation);
        }
        boolean end = node.nodeType() == TroubleshootTypes.NodeType.END;
        var path = pathTo(nodeList, byKey, node.nodeKey());
        var fc = new FlowControl(flow.id(), node.nodeKey(), node.nodeType().name(), node.expectedInput(), node.risk().name(), path, nodeList.size(), end, false);
        return new Answer(Intent.TROUBLESHOOTING, node.prompt(), new ArrayList<>(node.sourceRefs()), node.expectedInput(), node.risk().name(), fc);
    }

    private TroubleshootTypes.Reply normalizeReply(String text, TroubleshootNode node) {
        var v = text.toLowerCase(Locale.ROOT);
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

    private List<String> pathTo(List<TroubleshootNode> list, Map<String, TroubleshootNode> byKey, String target) {
        var path = new ArrayList<String>();
        var seen = new HashSet<String>();
        var cur = list.get(0);
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

    record Send(@NotBlank @Size(max = 4000) String content, @Size(max = 100) String errorCode) {
    }

    record ChangeProduct(@NotNull UUID productModelId, @Size(max = 80) String hardwareVersion,
                         @Size(max = 80) String firmwareVersion) {
    }

    record Feedback(boolean resolved, @Size(max = 1000) String comment) {
    }

    record AttachmentView(UUID messageId, UUID attachmentId) {
    }

    record Answer(Intent intent, String content, List<String> citations, String expectedInput, String risk,
                  FlowControl flowControl) {
    }

    record FlowControl(UUID flowId, String nodeKey, String nodeType, String expectedInput, String risk,
                       List<String> path, int totalSteps, boolean end, boolean escalated) {
    }

    record View(UUID id, UUID productModelId, String language, String region) {
    }

    record MessageView(UUID id, String content, String errorCode, Instant createdAt) {
        static MessageView of(ConversationMessage message) {
            return new MessageView(message.id(), message.content(), message.errorCode(), message.createdAt());
        }
    }
}
