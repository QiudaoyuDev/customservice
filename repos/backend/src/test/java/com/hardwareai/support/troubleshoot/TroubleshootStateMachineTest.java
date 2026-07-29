package com.hardwareai.support.troubleshoot;

import org.junit.jupiter.api.Test;

import static com.hardwareai.support.troubleshoot.TroubleshootTypes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TroubleshootStateMachineTest {
    private final TroubleshootStateMachine machine = new TroubleshootStateMachine();

    @Test
    void safetyStopsAlwaysEscalate() {
        assertTrue(machine.next(NodeType.OPERATION, Risk.HIGH, Reply.YES, "a", "b", "c", 0).escalated());
    }

    @Test
    void twoFailuresEscalate() {
        assertTrue(machine.next(NodeType.QUESTION, Risk.LOW, Reply.NO, "a", "b", "c", 2).escalated());
    }

    @Test
    void normalBranchUsesServerMapping() {
        assertEquals("b", machine.next(NodeType.QUESTION, Risk.LOW, Reply.NO, "a", "b", "c", 0).nextNodeKey());
    }

    @Test
    void refusalAlwaysEscalates() {
        assertTrue(machine.next(NodeType.QUESTION, Risk.LOW, Reply.REFUSE, "a", "b", "c", 0).escalated());
    }

    @Test
    void missingRequiredBranchEscalatesInsteadOfSilentlyEnding() {
        assertTrue(machine.next(NodeType.QUESTION, Risk.LOW, Reply.UNKNOWN, "a", "b", null, 0).escalated());
    }

    @Test
    void unknownAtFailureThresholdEscalates() {
        assertTrue(machine.next(NodeType.QUESTION, Risk.LOW, Reply.UNKNOWN, "a", "b", "c", 2).escalated());
    }
}
