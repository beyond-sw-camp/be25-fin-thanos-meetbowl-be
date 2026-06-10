package com.meetbowl.domain.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 건물 기준정보 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface BuildingRepositoryPort {

    Building save(Building building);

    Optional<Building> findById(UUID id);

    List<Building> findAll();

    List<Building> findBySiteId(UUID siteId);

    void deleteById(UUID id);
}
