package com.hardwareai.support.knowledge;
import java.util.*;
import org.springframework.data.jpa.repository.*;
interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
  Optional<ProcessingJob> findFirstByStatusOrderByCreatedAtAsc(ProcessingJob.Status status);
}
