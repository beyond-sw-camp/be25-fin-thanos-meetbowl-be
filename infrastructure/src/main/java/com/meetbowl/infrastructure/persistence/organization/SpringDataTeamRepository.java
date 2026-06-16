package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTeamRepository extends JpaRepository<TeamEntity, UUID> {

    boolean existsByDepartmentIdAndNameIgnoreCase(UUID departmentId, String name);

    boolean existsByDepartmentIdAndNameIgnoreCaseAndIdNot(UUID departmentId, String name, UUID id);
}
