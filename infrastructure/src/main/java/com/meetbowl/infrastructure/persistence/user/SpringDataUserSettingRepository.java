package com.meetbowl.infrastructure.persistence.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserSettingRepository extends JpaRepository<UserSettingEntity, UUID> {
    Optional<UserSettingEntity> findByUserId(UUID userId);
}
