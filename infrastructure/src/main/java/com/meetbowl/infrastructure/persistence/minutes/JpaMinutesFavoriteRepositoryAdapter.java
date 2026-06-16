package com.meetbowl.infrastructure.persistence.minutes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.minutes.MinutesFavorite;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;

/** 회의록 즐겨찾기 저장소 포트를 JPA로 구현한다. */
@Repository
public class JpaMinutesFavoriteRepositoryAdapter implements MinutesFavoriteRepositoryPort {

    private final SpringDataMinutesFavoriteRepository repository;

    public JpaMinutesFavoriteRepositoryAdapter(SpringDataMinutesFavoriteRepository repository) {
        this.repository = repository;
    }

    @Override
    public MinutesFavorite save(MinutesFavorite favorite) {
        return repository.save(MinutesFavoriteEntity.from(favorite)).toDomain();
    }

    @Override
    public Optional<MinutesFavorite> findByUserIdAndMinutesId(UUID userId, UUID minutesId) {
        return repository
                .findByUserIdAndMinutesId(userId, minutesId)
                .map(MinutesFavoriteEntity::toDomain);
    }

    @Override
    public List<MinutesFavorite> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByFavoritedAtDesc(userId).stream()
                .map(MinutesFavoriteEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByUserIdAndMinutesId(UUID userId, UUID minutesId) {
        repository.deleteByUserIdAndMinutesId(userId, minutesId);
    }
}
