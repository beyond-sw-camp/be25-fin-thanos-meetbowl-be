package com.meetbowl.infrastructure.persistence.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.auth.LoginSession;
import com.meetbowl.domain.auth.LoginSessionRepositoryPort;

@Repository
public class JpaLoginSessionRepositoryAdapter implements LoginSessionRepositoryPort {
    private final SpringDataLoginSessionRepository repository;

    public JpaLoginSessionRepositoryAdapter(SpringDataLoginSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public LoginSession save(LoginSession loginSession) {
        return repository.save(LoginSessionEntity.from(loginSession)).toDomain();
    }

    @Override
    public Optional<LoginSession> findBySessionTokenId(String sessionTokenId) {
        return repository.findBySessionTokenId(sessionTokenId).map(LoginSessionEntity::toDomain);
    }

    @Override
    public List<LoginSession> findActiveByUserId(UUID userId) {
        return repository.findByUserIdAndActiveTrue(userId).stream()
                .map(LoginSessionEntity::toDomain)
                .toList();
    }
}
