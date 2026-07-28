package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
    /**
     * PostgreSQL row locking makes each durable job owned by exactly one worker.
     */
    @Transactional
    @Query(value = """
            WITH candidate AS (
              SELECT id FROM index_jobs WHERE status = 'PENDING'
              ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
            )
            UPDATE index_jobs j SET status = 'RUNNING', attempts = attempts + 1, started_at = now()
            FROM candidate WHERE j.id = candidate.id RETURNING j.*
            """, nativeQuery = true)
    Optional<ProcessingJob> claimNext();
}
