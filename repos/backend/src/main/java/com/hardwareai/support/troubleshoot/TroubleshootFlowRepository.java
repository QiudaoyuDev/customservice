package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.llm.Intent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TroubleshootFlowRepository extends JpaRepository<TroubleshootFlow, UUID> {
    List<TroubleshootFlow> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);

    Optional<TroubleshootFlow> findByIdAndTenantId(UUID id, UUID tenant);

    @Query("""
            select f from TroubleshootFlow f
            where f.tenantId = :tenant and f.productModelId = :product and f.region = :region
              and f.locale = :locale and f.triggerIntent = :trigger
              and f.status = com.hardwareai.support.troubleshoot.TroubleshootFlow.Status.PUBLISHED
            """)
    Optional<TroubleshootFlow> findPublishedMatch(
            @Param("tenant") UUID tenant,
            @Param("product") UUID product,
            @Param("region") String region,
            @Param("locale") String locale,
            @Param("trigger") Intent trigger
    );
}
