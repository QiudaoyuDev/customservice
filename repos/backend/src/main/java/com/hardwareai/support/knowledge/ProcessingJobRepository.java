package com.hardwareai.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
              SELECT id FROM index_jobs
              WHERE (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= now()))
                 OR (status = 'RUNNING' AND lease_until < now())
              ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
            )
            UPDATE index_jobs j SET status = 'RUNNING', attempts = attempts + 1, started_at = now(),
              heartbeat_at = now(), lease_until = now() + interval '90 seconds', next_retry_at = null
            FROM candidate WHERE j.id = candidate.id RETURNING j.*
            """, nativeQuery = true)
    Optional<ProcessingJob> claimNext();

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE index_jobs SET heartbeat_at = now(), lease_until = now() + interval '90 seconds'
            WHERE id = :id AND status = 'RUNNING'
            """, nativeQuery = true)
    void heartbeat(@org.springframework.data.repository.query.Param("id") UUID id);
}
