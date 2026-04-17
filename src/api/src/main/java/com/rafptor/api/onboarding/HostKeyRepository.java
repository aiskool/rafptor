package com.rafptor.api.onboarding;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Repository for SSH host key fingerprints (TOFU store). */
public interface HostKeyRepository extends MongoRepository<HostKey, String> {

    Optional<HostKey> findByHostname(String hostname);
}
