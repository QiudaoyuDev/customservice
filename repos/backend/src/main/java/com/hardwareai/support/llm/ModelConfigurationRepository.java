package com.hardwareai.support.llm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ModelConfigurationRepository extends JpaRepository<ModelConfiguration, UUID> {
    List<ModelConfiguration> findAllByTenantIdOrderByName(UUID tenantId);
    Optional<ModelConfiguration> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<ModelConfiguration> findByTenantIdAndDefaultConfigTrueAndEnabledTrue(UUID tenantId);
}
