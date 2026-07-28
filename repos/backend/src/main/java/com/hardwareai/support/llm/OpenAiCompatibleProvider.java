package com.hardwareai.support.llm;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.*;
/** Minimal OpenAI-compatible provider; credentials are supplied at call time and never logged. */
@Component
public class OpenAiCompatibleProvider {
 public String complete(String baseUrl,String apiKey,String model,String system,String prompt){
  var response=RestClient.builder().baseUrl(baseUrl).defaultHeader("Authorization","Bearer "+apiKey).build().post().uri("/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
   .body(Map.of("model",model,"temperature",0,"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",prompt))))
   .retrieve().body(Map.class);
  var choices=(List<?>)response.get("choices"); if(choices==null||choices.isEmpty()) throw new IllegalStateException("LLM returned no choice");
  return String.valueOf(((Map<?,?>)((Map<?,?>)choices.getFirst()).get("message")).get("content"));
 }
}
