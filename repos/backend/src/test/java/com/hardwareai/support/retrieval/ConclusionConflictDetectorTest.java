package com.hardwareai.support.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConclusionConflictDetectorTest {
    private final ConclusionConflictDetector detector = new ConclusionConflictDetector();

    @Test
    void detectsOppositeInstructionsForSameSafetyAction() {
        assertTrue(detector.conflicts(List.of("Do not charge a swollen battery.", "It is safe to charge the battery.")));
    }

    @Test
    void doesNotFlagUnrelatedAdvice() {
        assertFalse(detector.conflicts(List.of("Do not disassemble the power supply.", "You can reset the device.")));
    }
}
