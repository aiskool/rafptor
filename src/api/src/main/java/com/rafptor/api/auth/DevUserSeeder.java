package com.rafptor.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Seeds a dev-only admin account when the application starts with the {@code dev}
 * profile, so the dashboard can be visited locally without a separate provisioning
 * step. Never runs in prod (profile gate).
 */
@Component
@Profile("dev")
public class DevUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final String email;
    private final String password;
    private final String tenantId;

    public DevUserSeeder(
            UserService userService,
            UserRepository userRepository,
            @Value("${rafptor.dev.user.email:aiskoolapp@gmail.com}") String email,
            @Value("${rafptor.dev.user.password:Test1234!devseed}") String password,
            @Value("${rafptor.dev.user.tenant:rafptor-demo}") String tenantId) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.email = email;
        this.password = password;
        this.tenantId = tenantId;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmailAndTenantId(email, tenantId).isPresent()) {
            log.info("dev user already present — email={} tenant={}", email, tenantId);
            return;
        }
        userService.register(email, password, tenantId, EnumSet.of(UserRole.ADMIN));
        log.info("dev user seeded — email={} tenant={}", email, tenantId);
    }
}
