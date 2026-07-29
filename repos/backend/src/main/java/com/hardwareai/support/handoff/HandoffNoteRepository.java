package com.hardwareai.support.handoff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HandoffNoteRepository extends JpaRepository<HandoffNote, UUID> {
    List<HandoffNote> findAllByHandoffIdOrderByCreatedAtAsc(UUID handoffId);
}
