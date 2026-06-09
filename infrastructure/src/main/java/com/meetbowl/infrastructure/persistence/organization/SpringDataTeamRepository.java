package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTeamRepository extends JpaRepository<TeamEntity, UUID> {}
