package com.meetbowl.infrastructure.persistence.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

@Repository
public class JpaUserSettingRepositoryAdapter implements UserSettingRepositoryPort {
    private final SpringDataUserSettingRepository repository;

    public JpaUserSettingRepositoryAdapter(SpringDataUserSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserSetting save(UserSetting userSetting) {
        return repository.save(UserSettingEntity.from(userSetting)).toDomain();
    }

    @Override
    public Optional<UserSetting> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(UserSettingEntity::toDomain);
    }
}
