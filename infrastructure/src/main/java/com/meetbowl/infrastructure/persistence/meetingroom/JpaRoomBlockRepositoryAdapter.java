package com.meetbowl.infrastructure.persistence.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meetingroom.RoomBlock;
import com.meetbowl.domain.meetingroom.RoomBlockRepositoryPort;

/** RoomBlock domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaRoomBlockRepositoryAdapter implements RoomBlockRepositoryPort {

    private final SpringDataRoomBlockRepository springDataRoomBlockRepository;

    public JpaRoomBlockRepositoryAdapter(
            SpringDataRoomBlockRepository springDataRoomBlockRepository) {
        this.springDataRoomBlockRepository = springDataRoomBlockRepository;
    }

    @Override
    public RoomBlock save(RoomBlock roomBlock) {
        return springDataRoomBlockRepository.save(RoomBlockEntity.from(roomBlock)).toDomain();
    }

    @Override
    public Optional<RoomBlock> findById(UUID id) {
        return springDataRoomBlockRepository.findById(id).map(RoomBlockEntity::toDomain);
    }

    @Override
    public List<RoomBlock> findByRoomId(UUID roomId) {
        return springDataRoomBlockRepository.findByRoomId(roomId).stream()
                .map(RoomBlockEntity::toDomain)
                .toList();
    }

    @Override
    public List<RoomBlock> findOverlapping(UUID roomId, Instant from, Instant to) {
        return springDataRoomBlockRepository.findOverlapping(roomId, from, to).stream()
                .map(RoomBlockEntity::toDomain)
                .toList();
    }

    @Override
    public List<RoomBlock> findByRoomIdInAndRange(List<UUID> roomIds, Instant from, Instant to) {
        // 빈 목록으로 IN 절을 만들면 일부 DB/Hibernate에서 비효율/오류가 되므로 조회 자체를 건너뛴다.
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }
        return springDataRoomBlockRepository.findByRoomIdInAndRange(roomIds, from, to).stream()
                .map(RoomBlockEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataRoomBlockRepository.deleteById(id);
    }
}
