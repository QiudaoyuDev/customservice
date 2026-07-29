package com.hardwareai.support.troubleshoot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TroubleshootFlowDefinitionRepository extends JpaRepository<TroubleshootFlowDefinition, UUID> {
}
