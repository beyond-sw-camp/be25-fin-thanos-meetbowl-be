package com.meetbowl.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByLoginId(String loginId);
}
