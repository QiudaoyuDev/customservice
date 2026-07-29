package com.hardwareai.support.troubleshoot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Reads only immutable, published flow snapshots for customer-facing execution.
 * Editable flow entities must never be used to resume an active conversation.
 */
@Service
public class FlowVersionService {
    private final TroubleshootFlowVersionSnapshotRepository snapshots;
    private final ObjectMapper json;

    FlowVersionService(TroubleshootFlowVersionSnapshotRepository snapshots, ObjectMapper json) {
        this.snapshots = snapshots;
        this.json = json.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Definition latest(UUID flowId) {
        var snapshot = snapshots.findAllByFlowIdOrderByVersionNoDesc(flowId).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Published flow has no snapshot"));
        return toDefinition(snapshot);
    }

    public Definition byId(UUID snapshotId) {
        return toDefinition(snapshots.findById(snapshotId)
            .orElseThrow(() -> new IllegalStateException("Flow snapshot is unavailable")));
    }

    private Definition toDefinition(TroubleshootFlowVersionSnapshot snapshot) {
        try {
            var payload = json.readValue(snapshot.definition(), SnapshotPayload.class);
            var nodes = payload.nodes() == null ? List.<Node>of() : payload.nodes().stream()
                .sorted(Comparator.comparingInt(Node::orderIndex))
                .toList();
            if (nodes.isEmpty()) throw new IllegalStateException("Flow snapshot has no nodes");
            return new Definition(snapshot.id(), snapshot.flowId(), nodes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Flow snapshot is invalid", exception);
        }
    }

    record SnapshotPayload(List<Node> nodes) {
    }

    public record Definition(UUID snapshotId, UUID flowId, List<Node> nodes) {
        public Node start() {
            return nodes.stream().min(Comparator.comparingInt(Node::orderIndex))
                .orElseThrow(() -> new IllegalStateException("Flow snapshot has no start node"));
        }
    }

    public record Node(
        String nodeKey, String nodeType, String prompt, String risk, String expectedInput,
        String branchYes, String branchNo, String branchUnknown, String branchNext,
        boolean safetyStop, List<String> sourceRefs, int orderIndex
    ) {
    }
}
