package com.meetbowl.domain.organization;

import java.util.Optional;
import java.util.UUID;

public interface PositionRepositoryPort {

    Position save(Position position);

    Optional<Position> findById(UUID positionId);
}
