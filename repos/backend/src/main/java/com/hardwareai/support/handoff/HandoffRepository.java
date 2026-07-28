package com.hardwareai.support.handoff;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
interface HandoffRepository extends JpaRepository<HandoffRequest,UUID>{Optional<HandoffRequest> findByTenantIdAndIdempotencyKey(UUID tenantId,String key); List<HandoffRequest> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);}
