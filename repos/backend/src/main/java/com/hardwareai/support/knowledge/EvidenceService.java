package com.hardwareai.support.knowledge;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.*;
/** Server-side evidence selector; never accepts tenant scope from a browser request. */
@Service
public class EvidenceService {
 private final KnowledgeChunkRepository chunks;
 public EvidenceService(KnowledgeChunkRepository chunks){this.chunks=chunks;}
 public List<Evidence> find(UUID tenantId,UUID productModelId,String region,String language,String question){
  return chunks.keywordSearch(tenantId,productModelId,region,language,question,PageRequest.of(0,3)).stream().map(c->new Evidence(c.id(),c.sourceLabel(),c.content())).toList();
 }
 public record Evidence(UUID chunkId,String source,String text){}
}
