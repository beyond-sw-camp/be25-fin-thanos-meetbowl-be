package com.meetbowl.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserSettingRepositoryPort {

    UserSetting save(UserSetting userSetting);

    Optional<UserSetting> findByUserId(UUID userId);
}
