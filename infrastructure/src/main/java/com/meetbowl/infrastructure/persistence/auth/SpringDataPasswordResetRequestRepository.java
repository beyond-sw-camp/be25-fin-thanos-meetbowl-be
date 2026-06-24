package com.meetbowl.infrastructure.persistence.auth;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.auth.PasswordResetRequestStatus;

public interface SpringDataPasswordResetRequestRepository
        extends JpaRepository<PasswordResetRequestEntity, UUID> {

    List<PasswordResetRequestEntity> findAllByStatusOrderByRequestedAtDesc(
            PasswordResetRequestStatus status);

    List<PasswordResetRequestEntity> findAllByOrderByRequestedAtDesc();

    long countByStatus(PasswordResetRequestStatus status);
}
