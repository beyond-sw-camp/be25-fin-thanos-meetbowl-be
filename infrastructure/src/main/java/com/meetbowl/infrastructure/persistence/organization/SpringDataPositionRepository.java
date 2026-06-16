package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPositionRepository extends JpaRepository<PositionEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
