package com.meetbowl.infrastructure.persistence.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;

@Repository
public class JpaTeamRepositoryAdapter implements TeamRepositoryPort {

    private final SpringDataTeamRepository repository;

    public JpaTeamRepositoryAdapter(SpringDataTeamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Team save(Team team) {
        return repository.save(TeamEntity.from(team)).toDomain();
    }

    @Override
    public Optional<Team> findById(UUID teamId) {
        return repository.findById(teamId).map(TeamEntity::toDomain);
    }
}
