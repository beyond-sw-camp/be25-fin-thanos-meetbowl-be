package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepositoryPort {

    Position save(Position position);

    Optional<Position> findById(UUID positionId);

    List<Position> findAll();

    List<Position> findAllByIds(Collection<UUID> positionIds);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    boolean existsByNameAndIdNot(String name, UUID positionId);

    boolean existsByCodeAndIdNot(String code, UUID positionId);
}
