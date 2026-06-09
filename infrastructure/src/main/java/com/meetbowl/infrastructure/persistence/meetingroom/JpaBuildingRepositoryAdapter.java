package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;

/** Building domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaBuildingRepositoryAdapter implements BuildingRepositoryPort {

    private final SpringDataBuildingRepository springDataBuildingRepository;

    public JpaBuildingRepositoryAdapter(SpringDataBuildingRepository springDataBuildingRepository) {
        this.springDataBuildingRepository = springDataBuildingRepository;
    }

    @Override
    public Building save(Building building) {
        return springDataBuildingRepository.save(BuildingEntity.from(building)).toDomain();
    }

    @Override
    public Optional<Building> findById(UUID id) {
        return springDataBuildingRepository.findById(id).map(BuildingEntity::toDomain);
    }

    @Override
    public List<Building> findAll() {
        return springDataBuildingRepository.findAll().stream()
                .map(BuildingEntity::toDomain)
                .toList();
    }

    @Override
    public List<Building> findBySiteId(UUID siteId) {
        return springDataBuildingRepository.findBySiteId(siteId).stream()
                .map(BuildingEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataBuildingRepository.deleteById(id);
    }
}
