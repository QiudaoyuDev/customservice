package com.hardwareai.support.troubleshoot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TroubleshootFlowVersionSnapshotRepository extends JpaRepository<TroubleshootFlowVersionSnapshot, UUID> {
    List<TroubleshootFlowVersionSnapshot> findAllByFlowIdOrderByVersionNoDesc(UUID id);
}
