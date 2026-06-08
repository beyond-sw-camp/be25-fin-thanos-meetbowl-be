package com.meetbowl.infrastructure.persistence.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLoginSessionRepository extends JpaRepository<LoginSessionEntity, UUID> {
    Optional<LoginSessionEntity> findBySessionTokenId(String sessionTokenId);

    List<LoginSessionEntity> findByUserIdAndActiveTrue(UUID userId);
}
