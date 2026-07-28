package com.hardwareai.support.troubleshoot;

import org.springframework.stereotype.Service;

import static com.hardwareai.support.troubleshoot.TroubleshootTypes.*;

/**
 * Deterministically resolves all workflow branches and safety stops.
 */
@Service
public class TroubleshootStateMachine {
    public Transition next(NodeType type, Risk risk, Reply reply, String yes, String no, String unknown, int failures) {
        if (risk == Risk.HIGH || reply == Reply.REFUSE || failures >= 2)
            return new Transition("HUMAN_ESCALATION", true);
        String node = switch (reply) {
            case YES -> yes;
            case NO -> no;
            case UNKNOWN -> unknown;
            case REFUSE -> "HUMAN_ESCALATION";
        };
        return new Transition(node == null ? "HUMAN_ESCALATION" : node, false);
    }

    public record Transition(String nextNodeKey, boolean escalated) {
    }
}
