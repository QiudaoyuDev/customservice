package com.hardwareai.support.qr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface QrBindingRepository extends JpaRepository<QrBinding, UUID> {
    Optional<QrBinding> findByTokenHash(String hash);
}
