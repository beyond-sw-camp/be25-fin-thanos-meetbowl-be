package com.meetbowl.domain.organization;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {

    Team save(Team team);

    Optional<Team> findById(UUID teamId);
}
