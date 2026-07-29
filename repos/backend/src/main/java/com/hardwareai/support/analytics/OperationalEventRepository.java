package com.hardwareai.support.analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface OperationalEventRepository extends JpaRepository<OperationalEvent, UUID> { java.util.List<OperationalEvent> findAllByTenantIdAndCreatedAtAfter(java.util.UUID tenantId, java.time.Instant after); }
