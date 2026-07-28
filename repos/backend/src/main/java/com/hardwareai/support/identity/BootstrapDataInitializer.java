package com.hardwareai.support.identity;

import com.hardwareai.support.config.AppProperties;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates the first local administrator only when the users table is empty.
 */
@Component
class BootstrapDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);
    private final UserAccountRepository users;
    private final EntityManager entityManager;
    private final PasswordEncoder passwords;
    private final AppProperties properties;

    BootstrapDataInitializer(
            UserAccountRepository users,
            EntityManager entityManager,
            PasswordEncoder passwords,
            AppProperties properties
    ) {
        this.users = users;
        this.entityManager = entityManager;
        this.passwords = passwords;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;
        if (properties.bootstrap().adminPassword().startsWith("CHANGE_ME")) {
            log.warn(
                    "No bootstrap administrator was created: BOOTSTRAP_ADMIN_PASSWORD is not configured"
            );
            return;
        }
        UUID tenantId = UUID.randomUUID();
        entityManager
                .createNativeQuery("insert into tenants(id,name,status) values (?1,?2,'ACTIVE')")
                .setParameter(1, tenantId)
                .setParameter(2, properties.bootstrap().tenantName())
                .executeUpdate();
        users.save(
                new UserAccount(
                        UUID.randomUUID(),
                        tenantId,
                        properties.bootstrap().adminEmail(),
                        passwords.encode(properties.bootstrap().adminPassword()),
                        UserAccount.Role.ADMIN
                )
        );
        log.info("Created initial administrator for tenant {}", tenantId);
    }
}
