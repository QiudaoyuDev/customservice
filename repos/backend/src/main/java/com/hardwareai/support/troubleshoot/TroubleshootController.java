package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.common.CurrentUser;
import com.hardwareai.support.llm.Intent;
import com.hardwareai.support.troubleshoot.TroubleshootTypes.NodeType;
import com.hardwareai.support.troubleshoot.TroubleshootTypes.Risk;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tenant-scoped diagnostic flow administration. The runtime orchestration that consumes
 * published flows lives in ConversationController; this controller only manages content.
 */
@RestController
@RequestMapping("/api/flows")
public class TroubleshootController {

    private static final Logger log = LoggerFactory.getLogger(TroubleshootController.class);

    private final TroubleshootFlowRepository flows;
    private final TroubleshootNodeRepository nodes;
    private final CurrentUser current;
    private final TroubleshootFlowVersionSnapshotRepository snapshots;
    private final TroubleshootFlowDefinitionRepository definitions;
    private final ObjectMapper json;

    TroubleshootController(TroubleshootFlowRepository flows, TroubleshootNodeRepository nodes, CurrentUser current, TroubleshootFlowVersionSnapshotRepository snapshots, TroubleshootFlowDefinitionRepository definitions, ObjectMapper json) {
        this.flows = flows;
        this.nodes = nodes;
        this.current = current;
        this.snapshots = snapshots;
        this.definitions = definitions;
        this.json = json;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FlowView create(@Valid @RequestBody Create c) {
        var f = new TroubleshootFlow(current.tenantId(), c.title(), c.triggerIntent(), c.productModelId(), c.region(), c.locale());
        var definition = definitions.save(new TroubleshootFlowDefinition(current.tenantId(), c.title()));
        f.assignDefinition(definition.id(), 1);
        f.update(c.title(), c.triggerIntent(), c.productModelId(), c.productVariantId(), c.hardwareRevision(), c.region(), c.locale(),
                c.firmwareMin(), c.firmwareMax(), c.triggerPhrase(), c.priority());
        var saved = flows.save(f);
        log.info("Flow created id={} tenant={} title={} triggerIntent={} product={}", saved.id(), current.tenantId(), c.title(), c.triggerIntent(), c.productModelId());
        return FlowView.of(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public List<FlowView> list() {
        return flows.findAllByTenantIdOrderByCreatedAtDesc(current.tenantId()).stream().map(FlowView::of).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public FlowDetail get(@PathVariable UUID id) {
        var f = flows.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Flow not found"));
        var ns = nodes.findAllByFlowIdOrderByOrderIndexAsc(id);
        return new FlowDetail(FlowView.of(f), ns.stream().map(NodeView::of).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable UUID id, @Valid @RequestBody UpdateMeta u) {
        var f = flows.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Flow not found"));
        editable(f);
        f.update(u.title(), u.triggerIntent(), u.productModelId(), u.productVariantId(), u.hardwareRevision(), u.region(), u.locale(),
                u.firmwareMin(), u.firmwareMax(), u.triggerPhrase(), u.priority());
        flows.save(f);
    }

    @PostMapping("/{id}/nodes")
    @PreAuthorize("hasRole('ADMIN')")
    public NodeView addNode(@PathVariable UUID id, @Valid @RequestBody NodeCreate c) {
        editable(owned(id));
        if (nodes.findByFlowIdAndNodeKey(id, c.nodeKey()).isPresent())
            throw new IllegalStateException("nodeKey already exists in this flow");
        var n = new TroubleshootNode(id, c.nodeKey());
        n.orderIndex(nodes.findAllByFlowIdOrderByOrderIndexAsc(id).stream().mapToInt(TroubleshootNode::orderIndex).max().orElse(-1) + 1);
        n.apply(c.nodeType(), c.prompt(), c.risk(), c.expectedInput(), c.branchYes(), c.branchNo(), c.branchUnknown(), c.branchNext(), c.safetyStop(), c.sourceRefs());
        return NodeView.of(nodes.save(n));
    }

    @PutMapping("/{id}/nodes/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateNode(@PathVariable UUID id, @PathVariable String key, @Valid @RequestBody NodeUpdate u) {
        editable(owned(id));
        var n = nodes.findByFlowIdAndNodeKey(id, key).orElseThrow(() -> new IllegalArgumentException("Node not found"));
        n.apply(u.nodeType(), u.prompt(), u.risk(), u.expectedInput(), u.branchYes(), u.branchNo(), u.branchUnknown(), u.branchNext(), u.safetyStop(), u.sourceRefs());
        nodes.save(n);
    }

    @DeleteMapping("/{id}/nodes/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteNode(@PathVariable UUID id, @PathVariable String key) {
        editable(owned(id));
        nodes.deleteByFlowIdAndNodeKey(id, key);
        nodes.findAllByFlowIdOrderByOrderIndexAsc(id).forEach(n -> {
            if (n.branchYes() != null && n.branchYes().equals(key)
                    || n.branchNo() != null && n.branchNo().equals(key)
                    || n.branchUnknown() != null && n.branchUnknown().equals(key)
                    || n.branchNext() != null && n.branchNext().equals(key)) {
                n.clearBranch(key);
                nodes.save(n);
            }
        });
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void submit(@PathVariable UUID id) {
        var f = owned(id);
        f.submit();
        flows.save(f);
        log.info("Flow submitted id={}", id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void approve(@PathVariable UUID id) {
        var f = owned(id);
        f.approve(current.userId());
        flows.save(f);
        log.info("Flow approved id={} by={}", id, current.userId());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void publish(@PathVariable UUID id) {
        var f = owned(id);
        validateForPublish(f);
        try {
            int version = snapshots.findAllByFlowIdOrderByVersionNoDesc(f.id()).stream().mapToInt(TroubleshootFlowVersionSnapshot::versionNo).max().orElse(0) + 1;
            snapshots.save(new TroubleshootFlowVersionSnapshot(f.id(), version, json.writeValueAsString(new FlowDetail(FlowView.of(f), nodes.findAllByFlowIdOrderByOrderIndexAsc(f.id()).stream().map(NodeView::of).toList()))));
        } catch (Exception exception) { throw new IllegalStateException("Unable to snapshot flow version", exception); }
        f.publish(current.userId());
        flows.save(f);
        log.info("Flow published id={} by={}", id, current.userId());
    }

    @PostMapping("/{id}/deprecate")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void deprecate(@PathVariable UUID id) {
        var f = owned(id);
        f.deprecate();
        flows.save(f);
        log.info("Flow deprecated id={}", id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public void restore(@PathVariable UUID id) {
        var f = owned(id);
        f.restore(current.userId());
        flows.save(f);
        log.info("Flow restored id={} by={}", id, current.userId());
    }

    @PostMapping("/{id}/simulate")
    @PreAuthorize("hasAnyRole('ADMIN','KNOWLEDGE_REVIEWER')")
    public SimulateResponse simulate(@PathVariable UUID id) {
        var f = owned(id);
        return simulate(f, nodes.findAllByFlowIdOrderByOrderIndexAsc(id));
    }

    private TroubleshootFlow owned(UUID id) {
        return flows.findByIdAndTenantId(id, current.tenantId()).orElseThrow(() -> new IllegalArgumentException("Flow not found"));
    }

    /** Published flow revisions are made by cloning into a new DRAFT; source content remains immutable. */
    @PostMapping({"/{id}/clone", "/{id}/versions"})
    @PreAuthorize("hasRole('ADMIN')")
    public FlowView cloneToDraft(@PathVariable UUID id) {
        var source = owned(id);
        var draft = new TroubleshootFlow(current.tenantId(), source.title() + " (revision)", source.triggerIntent(), source.productModelId(), source.region(), source.locale());
        UUID definitionId = source.definitionId();
        if (definitionId == null) {
            var definition = definitions.save(new TroubleshootFlowDefinition(current.tenantId(), source.title()));
            definitionId = definition.id();
            source.assignDefinition(definitionId, 1);
            flows.save(source);
        }
        int nextVersion = flows.findAllByDefinitionIdOrderByVersionNoDesc(definitionId).stream().mapToInt(TroubleshootFlow::versionNo).max().orElse(0) + 1;
        draft.assignDefinition(definitionId, nextVersion);
        draft.update(draft.title(), source.triggerIntent(), source.productModelId(), source.productVariantId(), source.hardwareRevision(), source.region(), source.locale(),
                source.firmwareMin(), source.firmwareMax(), source.triggerPhrase(), source.priority());
        draft = flows.save(draft);
        for (var sourceNode : nodes.findAllByFlowIdOrderByOrderIndexAsc(source.id())) {
            var copied = new TroubleshootNode(draft.id(), sourceNode.nodeKey());
            copied.orderIndex(sourceNode.orderIndex());
            copied.apply(sourceNode.nodeType(), sourceNode.prompt(), sourceNode.risk(), sourceNode.expectedInput(), sourceNode.branchYes(), sourceNode.branchNo(), sourceNode.branchUnknown(), sourceNode.branchNext(), sourceNode.safetyStop(), sourceNode.sourceRefs());
            nodes.save(copied);
        }
        log.info("Flow version cloned definition={} source={} draft={} version={} tenant={}", definitionId, source.id(), draft.id(), nextVersion, current.tenantId());
        return FlowView.of(draft);
    }

    private void editable(TroubleshootFlow flow) {
        if (flow.status() != TroubleshootFlow.Status.DRAFT)
            throw new IllegalStateException("Only draft flows can be edited; create a new draft for published content");
    }

    private void validateForPublish(TroubleshootFlow flow) {
        var flowNodes = nodes.findAllByFlowIdOrderByOrderIndexAsc(flow.id());
        if (flowNodes.isEmpty()) throw new IllegalStateException("A published flow requires at least one node");
        var keys = flowNodes.stream().map(TroubleshootNode::nodeKey).collect(Collectors.toSet());
        long startNodes = flowNodes.stream().filter(node -> node.orderIndex() == 0).count();
        if (startNodes != 1) throw new IllegalStateException("A published flow requires exactly one start node");
        for (var node : flowNodes) {
            for (String branch : new String[]{node.branchYes(), node.branchNo(), node.branchUnknown(), node.branchNext()}) {
                if (branch != null && !keys.contains(branch)) throw new IllegalStateException("Flow branch points to an unknown node: " + branch);
            }
            if (node.nodeType() != NodeType.END && node.nodeType() != NodeType.HUMAN_ESCALATION && node.branchYes() == null && node.branchNo() == null && node.branchUnknown() == null && node.branchNext() == null)
                throw new IllegalStateException("Non-terminal node requires at least one branch: " + node.nodeKey());
        }
        var reachable = reachable(flowNodes);
        if (reachable.size() != flowNodes.size()) throw new IllegalStateException("Published flow contains unreachable nodes");
        if (hasUncontrolledCycle(flowNodes)) throw new IllegalStateException("Published flow contains a cycle without an exit");
        boolean terminal = flowNodes.stream().anyMatch(n -> n.nodeType() == NodeType.END || n.nodeType() == NodeType.HUMAN_ESCALATION || n.risk() == Risk.HIGH || n.safetyStop());
        if (!terminal) throw new IllegalStateException("A published flow requires an end or human escalation path");
    }

    private Set<String> reachable(List<TroubleshootNode> nodes) {
        var byKey = nodes.stream().collect(Collectors.toMap(TroubleshootNode::nodeKey, node -> node));
        var visited = new HashSet<String>(); var queue = new ArrayDeque<String>(); queue.add(nodes.stream().min(Comparator.comparingInt(TroubleshootNode::orderIndex)).orElseThrow().nodeKey());
        while (!queue.isEmpty()) { var key = queue.remove(); if (!visited.add(key)) continue; var node = byKey.get(key); for (var branch : new String[]{node.branchYes(), node.branchNo(), node.branchUnknown(), node.branchNext()}) if (branch != null) queue.add(branch); }
        return visited;
    }
    private boolean hasUncontrolledCycle(List<TroubleshootNode> nodes) {
        var byKey = nodes.stream().collect(Collectors.toMap(TroubleshootNode::nodeKey, node -> node));
        return nodes.stream().anyMatch(start -> loopsWithoutTerminal(start.nodeKey(), byKey, new HashSet<>(), new HashSet<>()));
    }
    private boolean loopsWithoutTerminal(String key, Map<String, TroubleshootNode> nodes, Set<String> visiting, Set<String> done) {
        if (!done.add(key)) return visiting.contains(key); var node = nodes.get(key); if (node == null || node.nodeType() == NodeType.END || node.nodeType() == NodeType.HUMAN_ESCALATION || node.safetyStop()) return false;
        visiting.add(key); boolean loop = false; for (var branch : new String[]{node.branchYes(), node.branchNo(), node.branchUnknown(), node.branchNext()}) if (branch != null) loop |= loopsWithoutTerminal(branch, nodes, visiting, done); visiting.remove(key); return loop;
    }

    private SimulateResponse simulate(TroubleshootFlow flow, List<TroubleshootNode> ns) {
        var byKey = ns.stream().collect(Collectors.toMap(TroubleshootNode::nodeKey, n -> n));
        var start = ns.isEmpty() ? null : ns.get(0);
        var reachable = new HashSet<String>();
        if (start != null) {
            var q = new ArrayDeque<String>();
            q.add(start.nodeKey());
            reachable.add(start.nodeKey());
            while (!q.isEmpty()) {
                var k = q.poll();
                var n = byKey.get(k);
                if (n == null) continue;
                for (String b : new String[]{n.branchYes(), n.branchNo(), n.branchUnknown(), n.branchNext()}) {
                    if (b != null && !reachable.contains(b)) {
                        reachable.add(b);
                        q.add(b);
                    }
                }
            }
        }
        var unreachable = ns.stream().map(TroubleshootNode::nodeKey).filter(k -> !reachable.contains(k)).collect(Collectors.toList());
        var transcript = new ArrayList<SimStep>();
        var seen = new HashSet<String>();
        var cur = start;
        boolean escalated = false;
        while (cur != null && !seen.contains(cur.nodeKey()) && transcript.size() < 100) {
            seen.add(cur.nodeKey());
            boolean isEsc = cur.nodeType() == NodeType.HUMAN_ESCALATION || cur.risk() == Risk.HIGH;
            transcript.add(new SimStep(cur.nodeKey(), cur.nodeType().name(), cur.prompt(), cur.expectedInput(), cur.risk().name(), isEsc));
            if (isEsc) {
                escalated = true;
                break;
            }
            if (cur.nodeType() == NodeType.END) break;
            String next = null;
            if (cur.branchYes() != null) next = cur.branchYes();
            else if (cur.branchNext() != null) next = cur.branchNext();
            else if (cur.branchNo() != null) next = cur.branchNo();
            else if (cur.branchUnknown() != null) next = cur.branchUnknown();
            cur = next == null ? null : byKey.get(next);
        }
        return new SimulateResponse(transcript, escalated, new Coverage(ns.size(), reachable.size(), unreachable));
    }

    /* ---------- records ---------- */

    record Create(
            @NotBlank @Size(max = 200) String title,
            Intent triggerIntent,
            @NotNull UUID productModelId,
            @NotBlank @Size(max = 16) String region,
            @NotBlank @Size(max = 16) String locale,
            UUID productVariantId, @Size(max = 80) String hardwareRevision,
            @Size(max = 80) String firmwareMin, @Size(max = 80) String firmwareMax,
            @Size(max = 500) String triggerPhrase, int priority
    ) {
    }

    record UpdateMeta(
            @NotBlank @Size(max = 200) String title,
            Intent triggerIntent,
            @NotNull UUID productModelId,
            @NotBlank @Size(max = 16) String region,
            @NotBlank @Size(max = 16) String locale,
            UUID productVariantId, @Size(max = 80) String hardwareRevision,
            String firmwareMin,
            String firmwareMax,
            @Size(max = 500) String triggerPhrase, int priority
    ) {
    }

    record NodeCreate(
            @NotBlank @Size(max = 80) String nodeKey,
            NodeType nodeType,
            String prompt,
            Risk risk,
            String expectedInput,
            String branchYes,
            String branchNo,
            String branchUnknown,
            String branchNext,
            boolean safetyStop,
            List<String> sourceRefs
    ) {
    }

    record NodeUpdate(
            NodeType nodeType,
            String prompt,
            Risk risk,
            String expectedInput,
            String branchYes,
            String branchNo,
            String branchUnknown,
            String branchNext,
            boolean safetyStop,
            List<String> sourceRefs
    ) {
    }

    record FlowView(
            UUID id, UUID definitionId, int versionNo, String title, String triggerIntent, UUID productModelId, UUID productVariantId, String hardwareRevision,
            String region, String locale, String firmwareMin, String firmwareMax, String triggerPhrase, int priority, String status, String owner
    ) {
        static FlowView of(TroubleshootFlow f) {
            return new FlowView(f.id(), f.definitionId(), f.versionNo(), f.title(), f.triggerIntent().name(), f.productModelId(), f.productVariantId(), f.hardwareRevision(), f.region(), f.locale(),
                    f.firmwareMin(), f.firmwareMax(), f.triggerPhrase(), f.priority(), f.status().name(), f.owner());
        }
    }

    record NodeView(
            UUID id, String nodeKey, String nodeType, String prompt, String risk, String expectedInput,
            String branchYes, String branchNo, String branchUnknown, String branchNext, boolean safetyStop,
            List<String> sourceRefs, int orderIndex
    ) {
        static NodeView of(TroubleshootNode n) {
            return new NodeView(n.id(), n.nodeKey(), n.nodeType().name(), n.prompt(), n.risk().name(), n.expectedInput(),
                    n.branchYes(), n.branchNo(), n.branchUnknown(), n.branchNext(), n.safetyStop(), n.sourceRefs(), n.orderIndex());
        }
    }

    record FlowDetail(FlowView flow, List<NodeView> nodes) {
    }

    record SimStep(String nodeKey, String nodeType, String prompt, String expectedInput, String risk,
                   boolean escalated) {
    }

    record Coverage(int nodes, int visited, List<String> unreachable) {
    }

    record SimulateResponse(List<SimStep> transcript, boolean escalated, Coverage coverage) {
    }
}
