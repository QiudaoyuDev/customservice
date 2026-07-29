package com.hardwareai.support.analytics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
/** Records a restricted allow-list of non-sensitive operational attributes. */
@Service
public class OperationalEventService {
 private static final Set<String> ALLOWED = Set.of("intent","outcome","citationCount","latencyMs","reason","status","errorCodePresent","flowVersion","nodeType","attachmentType");
 private final OperationalEventRepository events; private final ObjectMapper json;
 OperationalEventService(OperationalEventRepository events,ObjectMapper json){this.events=events;this.json=json;}
 public void record(UUID tenantId, UUID conversationId, String type, Map<String,Object> attributes){try{var safe=new java.util.LinkedHashMap<String,Object>();attributes.forEach((k,v)->{if(ALLOWED.contains(k)&&v!=null)safe.put(k,v);});events.save(new OperationalEvent(tenantId,conversationId,type,json.writeValueAsString(safe)));}catch(Exception ignored){}}
}
