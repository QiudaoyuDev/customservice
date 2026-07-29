package com.hardwareai.support.knowledge;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingJobTest {
    @Test
    void onlyExhaustedJobBecomesTerminalFailure() {
        var job = new ProcessingJob(UUID.randomUUID(), ProcessingJob.Type.INDEX);
        job.start();
        job.fail(new IllegalStateException());
        assertFalse(job.exhausted());
        job.start();
        job.fail(new IllegalStateException());
        assertFalse(job.exhausted());
        job.start();
        job.fail(new IllegalStateException());
        assertTrue(job.exhausted());
    }
}
