package com.hardwareai.support.troubleshoot;

import com.hardwareai.support.troubleshoot.TroubleshootTypes.NodeType;
import com.hardwareai.support.troubleshoot.TroubleshootTypes.Risk;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One step of a diagnostic flow. Branches reference sibling node keys within the same flow.
 * The model may translate and interpret the prompt, but never change the branch targets.
 */
@Entity
@Table(name = "troubleshoot_nodes")
public class TroubleshootNode {

    @Id
    private UUID id;

    @Column(name = "flow_id")
    private UUID flowId;

    @Column(name = "node_key")
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @Column(columnDefinition = "text")
    private String prompt;

    @Enumerated(EnumType.STRING)
    private Risk risk;

    @Column(name = "expected_input")
    private String expectedInput;

    @Column(name = "branch_yes")
    private String branchYes;

    @Column(name = "branch_no")
    private String branchNo;

    @Column(name = "branch_unknown")
    private String branchUnknown;

    @Column(name = "branch_next")
    private String branchNext;

    @Column(name = "safety_stop")
    private boolean safetyStop;

    @ElementCollection
    @CollectionTable(name = "troubleshoot_node_refs", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "source_ref")
    private List<String> sourceRefs = new ArrayList<>();

    @Column(name = "order_index")
    private int orderIndex;

    protected TroubleshootNode() {
    }

    TroubleshootNode(UUID flowId, String nodeKey) {
        id = UUID.randomUUID();
        this.flowId = flowId;
        this.nodeKey = nodeKey;
        this.nodeType = NodeType.QUESTION;
        this.risk = Risk.LOW;
        this.expectedInput = "yes_no_unknown";
        this.orderIndex = 0;
    }

    void apply(NodeType nodeType, String prompt, Risk risk, String expectedInput, String yes, String no, String unknown, String next, boolean safetyStop, List<String> refs) {
        this.nodeType = nodeType;
        this.prompt = prompt;
        this.risk = risk;
        this.expectedInput = expectedInput;
        this.branchYes = yes;
        this.branchNo = no;
        this.branchUnknown = unknown;
        this.branchNext = next;
        this.safetyStop = safetyStop;
        this.sourceRefs = refs == null ? new ArrayList<>() : new ArrayList<>(refs);
    }

    void clearBranch(String key) {
        if (key.equals(branchYes)) branchYes = null;
        if (key.equals(branchNo)) branchNo = null;
        if (key.equals(branchUnknown)) branchUnknown = null;
        if (key.equals(branchNext)) branchNext = null;
    }

    public UUID id() {
        return id;
    }

    public UUID flowId() {
        return flowId;
    }

    public String nodeKey() {
        return nodeKey;
    }

    public NodeType nodeType() {
        return nodeType;
    }

    public String prompt() {
        return prompt;
    }

    public Risk risk() {
        return risk;
    }

    public String expectedInput() {
        return expectedInput;
    }

    public String branchYes() {
        return branchYes;
    }

    public String branchNo() {
        return branchNo;
    }

    public String branchUnknown() {
        return branchUnknown;
    }

    public String branchNext() {
        return branchNext;
    }

    public boolean safetyStop() {
        return safetyStop;
    }

    public List<String> sourceRefs() {
        return sourceRefs;
    }

    public int orderIndex() {
        return orderIndex;
    }

    void orderIndex(int v) {
        this.orderIndex = v;
    }
}
