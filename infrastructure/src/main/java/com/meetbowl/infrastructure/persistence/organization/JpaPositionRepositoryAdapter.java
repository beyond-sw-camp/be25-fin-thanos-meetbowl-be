package com.meetbowl.infrastructure.persistence.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;

@Repository
public class JpaPositionRepositoryAdapter implements PositionRepositoryPort {
    private final SpringDataPositionRepository repository;

    public JpaPositionRepositoryAdapter(SpringDataPositionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Position save(Position position) {
        return repository.save(PositionEntity.from(position)).toDomain();
    }

    @Override
    public Optional<Position> findById(UUID positionId) {
        return repository.findById(positionId).map(PositionEntity::toDomain);
    }
}
