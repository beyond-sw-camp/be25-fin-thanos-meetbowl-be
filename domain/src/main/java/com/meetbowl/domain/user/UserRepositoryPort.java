package com.meetbowl.domain.user;

import com.meetbowl.domain.common.Paged;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Paged<User> findPage(String keyword, int page, int size);
}
