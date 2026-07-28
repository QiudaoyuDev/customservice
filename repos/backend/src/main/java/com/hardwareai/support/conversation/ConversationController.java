package com.hardwareai.support.conversation;

import com.hardwareai.support.qr.*;
import com.hardwareai.support.product.ProductRepository;
import com.hardwareai.support.knowledge.ObjectStorage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Public customer API. The QR token is the only authority for initial product scope. */
@RestController @RequestMapping("/public/conversations")
class ConversationController {
 private final QrBindingRepository bindings; private final ProductRepository products; private final ConversationRepository conversations; private final ConversationProductContextRepository contexts; private final ConversationMessageRepository messages; private final MessageAttachmentRepository attachments; private final ConversationFeedbackRepository feedback; private final ObjectStorage storage;
 ConversationController(QrBindingRepository bindings, ProductRepository products, ConversationRepository conversations, ConversationProductContextRepository contexts, ConversationMessageRepository messages, MessageAttachmentRepository attachments, ConversationFeedbackRepository feedback, ObjectStorage storage){this.bindings=bindings;this.products=products;this.conversations=conversations;this.contexts=contexts;this.messages=messages;this.attachments=attachments;this.feedback=feedback;this.storage=storage;}
 @PostMapping
 public View create(@Valid @RequestBody Create request){
  var binding=bindings.findByTokenHash(hash(request.qrToken())).filter(QrBinding::valid).orElseThrow(()->new IllegalArgumentException("QR token is invalid, revoked, or expired"));
  var product=products.findById(binding.productModelId()).orElseThrow(()->new IllegalArgumentException("Product is unavailable"));
  var conversation=conversations.save(new Conversation(binding.tenantId(),binding.id(),request.language(),product.region()));
  contexts.save(new ConversationProductContext(conversation.id(),binding.productModelId(),request.hardwareVersion(),request.firmwareVersion(),"QR"));
  return new View(conversation.id(),binding.productModelId(),request.language(),product.region());
 }
 @PostMapping("/{id}/messages")
 public MessageView send(@PathVariable UUID id,@Valid @RequestBody Send request){
  var conversation=conversations.findById(id).filter(c->c.status()==Conversation.Status.OPEN).orElseThrow(()->new IllegalArgumentException("Conversation is unavailable"));
  var message=messages.save(new ConversationMessage(conversation.id(),request.content(),request.errorCode()));
  return MessageView.of(message);
 }
 @PostMapping("/{id}/product-context")
 public void changeProduct(@PathVariable UUID id,@Valid @RequestBody ChangeProduct request){
  var conversation=conversations.findById(id).filter(c->c.status()==Conversation.Status.OPEN).orElseThrow(()->new IllegalArgumentException("Conversation is unavailable"));
  products.findByIdAndTenantId(request.productModelId(),conversation.tenantId()).orElseThrow(()->new IllegalArgumentException("Product is unavailable"));
  contexts.findAllByConversationIdAndActiveTrue(id).forEach(context->{context.close();contexts.save(context);});
  contexts.save(new ConversationProductContext(id,request.productModelId(),request.hardwareVersion(),request.firmwareVersion(),"USER_SELECTED"));
 }
 @PostMapping(value="/{id}/attachments",consumes="multipart/form-data")
 public AttachmentView uploadAttachment(@PathVariable UUID id,@RequestPart @NotBlank @Size(max=4000) String content,@RequestPart(required=false) @Size(max=100) String errorCode,@RequestPart MultipartFile file){
  if(file.isEmpty()||file.getSize()>10*1024*1024||!Set.of("image/png","image/jpeg").contains(file.getContentType())) throw new IllegalArgumentException("Only PNG or JPEG up to 10 MiB is supported");
  var conversation=conversations.findById(id).filter(c->c.status()==Conversation.Status.OPEN).orElseThrow(()->new IllegalArgumentException("Conversation is unavailable"));
  var message=messages.save(new ConversationMessage(conversation.id(),content,errorCode)); String key=conversation.tenantId()+"/conversations/"+id+"/"+UUID.randomUUID(); storage.put(key,file);
  var attachment=attachments.save(new MessageAttachment(message.id(),key,file.getContentType(),file.getSize())); return new AttachmentView(message.id(),attachment.id());
 }
 @PostMapping("/{id}/feedback") public void submitFeedback(@PathVariable UUID id,@Valid @RequestBody Feedback request){conversations.findById(id).orElseThrow(()->new IllegalArgumentException("Conversation is unavailable")); feedback.save(new ConversationFeedback(id,request.resolved(),request.comment()));}
 @GetMapping("/{id}/messages") public List<MessageView> history(@PathVariable UUID id){return messages.findAllByConversationIdOrderByCreatedAtAsc(id).stream().map(MessageView::of).toList();}
 private String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 record Create(@NotBlank String qrToken,@NotBlank @Size(max=16) String language,@Size(max=80) String hardwareVersion,@Size(max=80) String firmwareVersion){}
 record Send(@NotBlank @Size(max=4000) String content,@Size(max=100) String errorCode){}
 record ChangeProduct(@NotNull UUID productModelId,@Size(max=80) String hardwareVersion,@Size(max=80) String firmwareVersion){}
 record Feedback(boolean resolved,@Size(max=1000) String comment){}
 record AttachmentView(UUID messageId,UUID attachmentId){}
 record View(UUID id,UUID productModelId,String language,String region){}
 record MessageView(UUID id,String content,String errorCode,Instant createdAt){static MessageView of(ConversationMessage message){return new MessageView(message.id(),message.content(),message.errorCode(),message.createdAt());}}
}
