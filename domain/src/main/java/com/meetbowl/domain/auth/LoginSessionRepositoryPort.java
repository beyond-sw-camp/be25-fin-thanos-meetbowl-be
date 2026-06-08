package com.meetbowl.domain.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepositoryPort {

    LoginSession save(LoginSession loginSession);

    Optional<LoginSession> findBySessionTokenId(String sessionTokenId);

    List<LoginSession> findActiveByUserId(UUID userId);
}
