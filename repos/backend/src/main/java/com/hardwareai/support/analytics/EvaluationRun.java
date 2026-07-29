package com.hardwareai.support.analytics;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Immutable execution envelope for a repeatable evaluation batch. */
@Entity @Table(name = "evaluation_runs") class EvaluationRun {
 @Id private UUID id; @Column(name="tenant_id") private UUID tenantId; private String label; @Column(name="knowledge_version") private String knowledgeVersion; @Column(name="model_version") private String modelVersion; @Column(name="retrieval_version") private String retrievalVersion; @Column(name="created_at") private Instant createdAt=Instant.now(); protected EvaluationRun(){} EvaluationRun(UUID tenant,String label,String knowledge,String model,String retrieval){id=UUID.randomUUID();tenantId=tenant;this.label=label;knowledgeVersion=knowledge;modelVersion=model;retrievalVersion=retrieval;} UUID id(){return id;} UUID tenantId(){return tenantId;} String label(){return label;} Instant createdAt(){return createdAt;}
}
