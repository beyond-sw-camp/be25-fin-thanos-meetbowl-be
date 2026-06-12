package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {

    Team save(Team team);

    Optional<Team> findById(UUID teamId);

    List<Team> findAll();

    List<Team> findAllByIds(Collection<UUID> teamIds);

    boolean existsByDepartmentIdAndName(UUID departmentId, String name);

    boolean existsByDepartmentIdAndNameAndIdNot(UUID departmentId, String name, UUID teamId);
}
