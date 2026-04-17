package com.rafptor.api.onboarding;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Repository for {@link AgentToken} documents. */
public interface AgentTokenRepository extends MongoRepository<AgentToken, String> {

    Optional<AgentToken> findByTokenHash(String tokenHash);
}
