package com.meetbowl.infrastructure.persistence.organization;

import java.util.Collection;
import java.util.List;
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

    @Override
    public List<Team> findAll() {
        return repository.findAll().stream().map(TeamEntity::toDomain).toList();
    }

    @Override
    public List<Team> findAllForExcelExport() {
        return repository.findAllForExcelExport().stream().map(TeamEntity::toDomain).toList();
    }

    @Override
    public List<Team> findAllByIds(Collection<UUID> teamIds) {
        return repository.findAllById(teamIds).stream().map(TeamEntity::toDomain).toList();
    }

    @Override
    public boolean existsByDepartmentIdAndName(UUID departmentId, String name) {
        return repository.existsByDepartmentIdAndNameIgnoreCase(departmentId, name);
    }

    @Override
    public boolean existsByDepartmentIdAndNameAndIdNot(
            UUID departmentId, String name, UUID teamId) {
        return repository.existsByDepartmentIdAndNameIgnoreCaseAndIdNot(departmentId, name, teamId);
    }
}
