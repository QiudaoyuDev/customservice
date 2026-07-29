package com.hardwareai.support.handoff;

import com.hardwareai.support.analytics.OperationalEventService;
import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.conversation.ConversationAccessService;
import com.hardwareai.support.conversation.HandoffPackageBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User and operator actions share one idempotent, tenant-bound handoff record.
 */
@RestController
class HandoffController {
    private static final Logger log = LoggerFactory.getLogger(HandoffController.class);

    private final HandoffRepository requests;
    private final ConversationAccessService conversations;
    private final CurrentUser current;
    private final HandoffPackageBuilder packageBuilder;
    private final HandoffNoteRepository notes;
    private final OperationalEventService events;
    private final HandoffDeliveryService delivery;

    HandoffController(HandoffRepository requests, ConversationAccessService conversations, CurrentUser current,
        HandoffPackageBuilder packageBuilder, HandoffNoteRepository notes, OperationalEventService events,
        HandoffDeliveryService delivery) {
        this.requests = requests;
        this.conversations = conversations;
        this.current = current;
        this.packageBuilder = packageBuilder;
        this.notes = notes;
        this.events = events;
        this.delivery = delivery;
    }

    @PostMapping("/public/handoffs")
    View create(@RequestHeader("X-Conversation-Token") String accessToken, @Valid @RequestBody Create input) {
        UUID tenant = conversations.authorize(input.conversationId(), accessToken);
        var existing = requests.findByTenantIdAndIdempotencyKey(tenant, input.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Handoff deduplicated conversation={} id={}", input.conversationId(), existing.get().id());
            return View.of(existing.get());
        }
        var snapshot = packageBuilder.build(tenant, input.conversationId(), input.reason(), input.summary(), input.contact(),
            input.contactAuthorized());
        var item = requests.save(
            new HandoffRequest(tenant, input.conversationId(), input.idempotencyKey(), input.reason(), input.summary(),
                input.contact(), input.contactAuthorized(), snapshot));
        delivery.deliver(item);
        events.record(tenant, input.conversationId(), "HANDOFF_CREATED",
            java.util.Map.of("reason", input.reason(), "status", item.status().name()));
        log.info("Handoff created id={} tenant={} conversation={}", item.id(), tenant, input.conversationId());
        return View.of(item);
    }

    @GetMapping("/api/handoffs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    List<View> list() {
        return requests.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(View::of).toList();
    }

    @PostMapping("/api/handoffs/{id}/claim")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    void claim(@PathVariable UUID id) {
        var item = owned(id);
        item.claim(current.userId());
        requests.save(item);
        events.record(current.tenantId(), item.conversationId(), "HANDOFF_CLAIMED",
            java.util.Map.of("status", item.status().name()));
        log.info("Handoff claimed id={} by={}", id, current.userId());
    }

    @PostMapping("/api/handoffs/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    void changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange input) {
        var item = owned(id);
        item.transition(input.status());
        requests.save(item);
        events.record(current.tenantId(), item.conversationId(), "HANDOFF_STATUS_CHANGED",
            java.util.Map.of("status", item.status().name()));
        log.info("Handoff status changed id={} status={}", id, item.status());
    }

    @PostMapping("/api/handoffs/{id}/priority")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    void updatePriority(@PathVariable UUID id, @Valid @RequestBody PriorityChange input) {
        var item = owned(id);
        item.reprioritize(input.priority(), input.slaDueAt());
        requests.save(item);
        events.record(current.tenantId(), item.conversationId(), "HANDOFF_PRIORITY_CHANGED",
            java.util.Map.of("priority", item.priority().name()));
    }

    @PostMapping("/api/handoffs/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    void close(@PathVariable UUID id, @Valid @RequestBody Close input) {
        var item = owned(id);
        item.close(input.resolution());
        requests.save(item);
        events.record(current.tenantId(), item.conversationId(), "HANDOFF_CLOSED",
            java.util.Map.of("status", item.status().name(), "outcome", input.resolution().name()));
        log.info("Handoff closed id={}", id);
    }

    @GetMapping("/api/handoffs/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    List<NoteView> notes(@PathVariable UUID id) {
        owned(id);
        return notes.findAllByHandoffIdOrderByCreatedAtAsc(id).stream().map(NoteView::of).toList();
    }

    @PostMapping("/api/handoffs/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    NoteView addNote(@PathVariable UUID id, @Valid @RequestBody Note input) {
        owned(id);
        var note = notes.save(new HandoffNote(id, current.userId(), input.content()));
        log.info("Handoff note added handoff={} author={}", id, current.userId());
        return NoteView.of(note);
    }

    private HandoffRequest owned(UUID id) {
        var item = requests.findById(id).orElseThrow(() -> new IllegalArgumentException("Handoff not found"));
        if (!item.tenantId().equals(current.tenantId())) throw new IllegalArgumentException("Handoff not found");
        return item;
    }

    record Create(@NotNull UUID conversationId, @NotBlank @Size(max = 160) String idempotencyKey,
                  @NotBlank @Size(max = 300) String reason, @NotBlank @Size(max = 8000) String summary,
                  @Size(max = 300) String contact,
                  boolean contactAuthorized) {
    }

    record Close(@NotNull HandoffRequest.Resolution resolution) {
    }

    record StatusChange(@NotNull HandoffRequest.Status status) {
    }

    record PriorityChange(@NotNull HandoffRequest.Priority priority, @NotNull java.time.Instant slaDueAt) {
    }

    record Note(@NotBlank @Size(max = 4000) String content) {
    }

    record NoteView(UUID id, UUID authorId, String content, java.time.Instant createdAt) {
        static NoteView of(HandoffNote note) {
            return new NoteView(note.id(), note.authorId(), note.content(), note.createdAt());
        }
    }

    record View(UUID id, UUID conversationId, String status, String reason, String summary, String contact,
                boolean contactAuthorized, UUID assignedTo, String resolution, java.time.Instant createdAt,
                java.time.Instant closedAt, String packageSnapshot, String priority, java.time.Instant slaDueAt) {
        static View of(HandoffRequest request) {
            return new View(request.id(), request.conversationId(), request.status().name(), request.reason(), request.summary(),
                request.contact(), request.contactAuthorized(), request.assignedTo(),
                request.resolution() == null ? null : request.resolution().name(),
                request.createdAt(), request.closedAt(), request.packageSnapshot(), request.priority().name(), request.slaDueAt());
        }
    }
}
