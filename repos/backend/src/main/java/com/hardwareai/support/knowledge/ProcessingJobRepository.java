package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.*;

import java.util.Optional;
import java.util.UUID;

interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
    Optional<ProcessingJob> findFirstByStatusOrderByCreatedAtAsc(ProcessingJob.Status status);
}
