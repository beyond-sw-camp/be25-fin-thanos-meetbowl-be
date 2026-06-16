package com.meetbowl.infrastructure.persistence.minutes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 회의록 즐겨찾기 엔티티의 Spring Data JPA 리포지토리다. */
interface SpringDataMinutesFavoriteRepository extends JpaRepository<MinutesFavoriteEntity, UUID> {

    Optional<MinutesFavoriteEntity> findByUserIdAndMinutesId(UUID userId, UUID minutesId);

    List<MinutesFavoriteEntity> findByUserIdOrderByFavoritedAtDesc(UUID userId);

    void deleteByUserIdAndMinutesId(UUID userId, UUID minutesId);
}
