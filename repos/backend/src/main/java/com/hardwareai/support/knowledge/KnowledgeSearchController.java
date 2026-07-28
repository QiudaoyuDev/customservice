package com.hardwareai.support.knowledge;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/** Admin-only retrieval probe; it proves applicability filters before conversational AI is introduced. */
@RestController
@RequestMapping("/api/search")
class KnowledgeSearchController {

  private final RestClient qdrant;
  private final RestClient embedding;
  private final AppProperties config;
  private final CurrentUser current;

  KnowledgeSearchController(AppProperties config, CurrentUser current) {
    this.config = config;
    this.current = current;
    qdrant = RestClient.builder()
      .baseUrl(config.qdrantUrl())
      .defaultHeader("api-key", config.qdrantApiKey())
      .build();
    embedding = RestClient.create(config.embeddingUrl());
  }

  @PostMapping
  public Map<?, ?> search(@Valid @RequestBody Query query) {
    var vector = embed(query.query());
    var filter = Map.of(
      "must",
      List.of(
        Map.of(
          "key",
          "productModelId",
          "match",
          Map.of("value", query.productModelId().toString())
        ),
        Map.of("key", "region", "match", Map.of("value", query.region())),
        Map.of("key", "status", "match", Map.of("value", "PUBLISHED"))
      )
    );
    return qdrant
      .post()
      .uri("/collections/{collection}/points/query", config.qdrantCollection())
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        Map.of(
          "query",
          vector,
          "limit",
          Math.min(query.limit(), 10),
          "with_payload",
          true,
          "filter",
          filter
        )
      )
      .retrieve()
      .body(Map.class);
  }

  @SuppressWarnings("unchecked")
  private List<Double> embed(String text) {
    var body = embedding
      .post()
      .uri("/v1/embeddings")
      .contentType(MediaType.APPLICATION_JSON)
      .body(Map.of("input", List.of(text), "normalize", true))
      .retrieve()
      .body(Map.class);
    return (List<Double>) ((Map<?, ?>) ((List<?>) body.get("data")).getFirst()).get("embedding");
  }

  record Query(
    @NotBlank @Size(max = 1000) String query,
    @NotNull UUID productModelId,
    @NotBlank @Size(max = 16) String region,
    @Min(1) @Max(10) int limit
  ) {}
}
