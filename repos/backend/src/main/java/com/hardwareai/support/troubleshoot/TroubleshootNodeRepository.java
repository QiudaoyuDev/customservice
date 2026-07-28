package com.hardwareai.support.troubleshoot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TroubleshootNodeRepository extends JpaRepository<TroubleshootNode, UUID> {
    List<TroubleshootNode> findAllByFlowIdOrderByOrderIndexAsc(UUID flowId);

    java.util.Optional<TroubleshootNode> findByFlowIdAndNodeKey(UUID flowId, String nodeKey);

    void deleteByFlowIdAndNodeKey(UUID flowId, String nodeKey);
}
