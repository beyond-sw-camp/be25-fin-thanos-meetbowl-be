package com.meetbowl.infrastructure.persistence.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.meetingroom.ReservationStatus;
import com.meetbowl.domain.meetingroom.RoomReservation;
import com.meetbowl.domain.meetingroom.RoomReservationRepositoryPort;

/** RoomReservation domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaRoomReservationRepositoryAdapter implements RoomReservationRepositoryPort {

    private final SpringDataRoomReservationRepository springDataRoomReservationRepository;

    public JpaRoomReservationRepositoryAdapter(
            SpringDataRoomReservationRepository springDataRoomReservationRepository) {
        this.springDataRoomReservationRepository = springDataRoomReservationRepository;
    }

    @Override
    public RoomReservation save(RoomReservation reservation) {
        RoomReservationEntity savedEntity =
                springDataRoomReservationRepository.save(RoomReservationEntity.from(reservation));
        return savedEntity.toDomain();
    }

    @Override
    public Optional<RoomReservation> findById(UUID id) {
        return springDataRoomReservationRepository
                .findById(id)
                .map(RoomReservationEntity::toDomain);
    }

    @Override
    public List<RoomReservation> findByReservedByUserId(UUID userId) {
        return springDataRoomReservationRepository.findByReservedByUserId(userId).stream()
                .map(RoomReservationEntity::toDomain)
                .toList();
    }

    @Override
    public List<RoomReservation> findActiveOverlaps(
            UUID meetingRoomId, Instant startedAt, Instant endedAt) {
        return springDataRoomReservationRepository
                .findOverlaps(meetingRoomId, ReservationStatus.RESERVED, startedAt, endedAt)
                .stream()
                .map(RoomReservationEntity::toDomain)
                .toList();
    }

    @Override
    public Paged<RoomReservation> search(
            UUID meetingRoomId, Instant from, Instant to, int page, int size) {
        // API page는 1부터, Spring Data는 0부터 시작하므로 변환한다.
        PageRequest pageRequest =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "startedAt"));
        Page<RoomReservationEntity> result =
                springDataRoomReservationRepository.search(meetingRoomId, from, to, pageRequest);
        List<RoomReservation> content =
                result.getContent().stream().map(RoomReservationEntity::toDomain).toList();
        return new Paged<>(content, result.getTotalElements());
    }

    @Override
    public Paged<RoomReservation> searchReservations(
            List<UUID> meetingRoomIds,
            ReservationStatus status,
            Instant from,
            Instant to,
            int page,
            int size) {
        PageRequest pageRequest =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "startedAt"));
        if (meetingRoomIds != null && meetingRoomIds.isEmpty()) {
            return new Paged<>(List.of(), 0);
        }
        Page<RoomReservationEntity> result =
                meetingRoomIds == null
                        ? springDataRoomReservationRepository.searchAdmin(
                                status, from, to, pageRequest)
                        : springDataRoomReservationRepository.searchAdminByRooms(
                                meetingRoomIds, status, from, to, pageRequest);
        List<RoomReservation> content =
                result.getContent().stream().map(RoomReservationEntity::toDomain).toList();
        return new Paged<>(content, result.getTotalElements());
    }

    @Override
    public boolean hasActiveReservations(UUID meetingRoomId) {
        return springDataRoomReservationRepository.existsByMeetingRoomIdAndStatus(
                meetingRoomId, ReservationStatus.RESERVED);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRoomReservationRepository.deleteById(id);
    }
}
