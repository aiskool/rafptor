package com.rafptor.api.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<RafptorUser, String> {
    Optional<RafptorUser> findByEmailAndTenantId(String email, String tenantId);
    Optional<RafptorUser> findByEmail(String email);
}
