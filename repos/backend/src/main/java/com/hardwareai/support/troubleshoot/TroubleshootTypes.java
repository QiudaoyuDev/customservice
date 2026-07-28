package com.hardwareai.support.troubleshoot;

/**
 * Closed server-side workflow vocabulary; an LLM can only supply a normalized response.
 */
public final class TroubleshootTypes {
    private TroubleshootTypes() {
    }

    public enum NodeType {QUESTION, OPERATION, DECISION, HUMAN_ESCALATION, END}

    public enum Reply {YES, NO, UNKNOWN, REFUSE}

    public enum Risk {LOW, MEDIUM, HIGH}
}
