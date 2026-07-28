package com.hardwareai.support.identity;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface UserAccountRepository extends JpaRepository<UserAccount, UUID> { Optional<UserAccount> findByEmail(String email); }
