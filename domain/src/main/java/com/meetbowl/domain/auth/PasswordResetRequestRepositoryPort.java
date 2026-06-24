package com.meetbowl.domain.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRequestRepositoryPort {

    PasswordResetRequest save(PasswordResetRequest request);

    Optional<PasswordResetRequest> findById(UUID requestId);

    List<PasswordResetRequest> findAllByStatus(PasswordResetRequestStatus status);

    List<PasswordResetRequest> findAll();

    long countByStatus(PasswordResetRequestStatus status);
}
