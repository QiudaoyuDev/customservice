package com.hardwareai.support.knowledge;

import com.hardwareai.support.config.AppProperties;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Qdrant adapter: application metadata is always embedded in the payload for retrieval filtering. */
@Service
class VectorIndex {
  private final RestClient client; private final AppProperties config;
  VectorIndex(AppProperties config){this.config=config;client=RestClient.builder().baseUrl(config.qdrantUrl()).defaultHeader("api-key",config.qdrantApiKey()).build();}
  void upsert(KnowledgeRevision revision){if(revision.extractedText()==null||revision.extractedText().isBlank())throw new IllegalStateException("Cannot index an empty revision");var vector=embedding(revision.extractedText());ensureCollection(vector.size());client.put().uri("/collections/{collection}/points?wait=true",config.qdrantCollection()).contentType(MediaType.APPLICATION_JSON).body(Map.of("points",List.of(Map.of("id",revision.id().toString(),"vector",vector,"payload",Map.of("revisionId",revision.id().toString(),"productModelId",revision.productModelId().toString(),"region",revision.region(),"status",revision.status().name(),"text",revision.extractedText()))))).retrieve().toBodilessEntity();}
  /** Creates the collection lazily because embedding dimensions are model-dependent. */
  private void ensureCollection(int vectorSize){boolean exists=client.get().uri("/collections/{collection}",config.qdrantCollection()).exchange((request,response)->response.getStatusCode().is2xxSuccessful());if(!exists)client.put().uri("/collections/{collection}",config.qdrantCollection()).contentType(MediaType.APPLICATION_JSON).body(Map.of("vectors",Map.of("size",vectorSize,"distance","Cosine"))).retrieve().toBodilessEntity();}
  @SuppressWarnings("unchecked") private List<Double> embedding(String text){var body=RestClient.create(config.embeddingUrl()).post().uri("/v1/embeddings").contentType(MediaType.APPLICATION_JSON).body(Map.of("input",List.of(text),"normalize",true)).retrieve().body(Map.class);return (List<Double>)((Map<?,?>)((List<?>)body.get("data")).getFirst()).get("embedding");}
}
