package com.hardwareai.support.handoff;
import com.hardwareai.support.common.CurrentUser; import com.hardwareai.support.conversation.ConversationAccessService;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
/** User and operator actions share one idempotent, tenant-bound handoff record. */
@RestController class HandoffController { private final HandoffRepository requests; private final ConversationAccessService conversations; private final CurrentUser current;
 HandoffController(HandoffRepository requests,ConversationAccessService conversations,CurrentUser current){this.requests=requests;this.conversations=conversations;this.current=current;}
 @PostMapping("/public/handoffs") View create(@Valid @RequestBody Create input){UUID tenant=conversations.tenantId(input.conversationId());var item=requests.findByTenantIdAndIdempotencyKey(tenant,input.idempotencyKey()).orElseGet(()->requests.save(new HandoffRequest(tenant,input.conversationId(),input.idempotencyKey(),input.reason(),input.summary(),input.contactAuthorized())));return View.of(item);}
 @GetMapping("/api/handoffs") @PreAuthorize("hasRole('ADMIN')") List<View> list(){return requests.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(View::of).toList();}
 @PostMapping("/api/handoffs/{id}/claim") @PreAuthorize("hasRole('ADMIN')") void claim(@PathVariable UUID id){var item=owned(id);item.claim(current.userId());requests.save(item);}
 @PostMapping("/api/handoffs/{id}/close") @PreAuthorize("hasRole('ADMIN')") void close(@PathVariable UUID id,@Valid @RequestBody Close input){var item=owned(id);item.close(input.resolution());requests.save(item);}
 private HandoffRequest owned(UUID id){var item=requests.findById(id).orElseThrow(()->new IllegalArgumentException("Handoff not found"));if(!item.tenantId().equals(current.tenantId()))throw new IllegalArgumentException("Handoff not found");return item;}
 record Create(@NotNull UUID conversationId,@NotBlank @Size(max=160) String idempotencyKey,@NotBlank @Size(max=300) String reason,@NotBlank @Size(max=8000) String summary,boolean contactAuthorized){} record Close(@NotNull HandoffRequest.Resolution resolution){} record View(UUID id){static View of(HandoffRequest request){return new View(request.id());}}
}
