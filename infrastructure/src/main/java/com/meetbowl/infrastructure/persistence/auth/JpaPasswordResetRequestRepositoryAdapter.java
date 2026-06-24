package com.meetbowl.infrastructure.persistence.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestRepositoryPort;
import com.meetbowl.domain.auth.PasswordResetRequestStatus;

@Repository
public class JpaPasswordResetRequestRepositoryAdapter implements PasswordResetRequestRepositoryPort {

    private final SpringDataPasswordResetRequestRepository repository;

    public JpaPasswordResetRequestRepositoryAdapter(
            SpringDataPasswordResetRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordResetRequest save(PasswordResetRequest request) {
        return repository.save(PasswordResetRequestEntity.from(request)).toDomain();
    }

    @Override
    public Optional<PasswordResetRequest> findById(UUID requestId) {
        return repository.findById(requestId).map(PasswordResetRequestEntity::toDomain);
    }

    @Override
    public List<PasswordResetRequest> findAllByStatus(PasswordResetRequestStatus status) {
        return repository.findAllByStatusOrderByRequestedAtDesc(status).stream()
                .map(PasswordResetRequestEntity::toDomain)
                .toList();
    }

    @Override
    public List<PasswordResetRequest> findAll() {
        return repository.findAllByOrderByRequestedAtDesc().stream()
                .map(PasswordResetRequestEntity::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(PasswordResetRequestStatus status) {
        return repository.countByStatus(status);
    }
}
