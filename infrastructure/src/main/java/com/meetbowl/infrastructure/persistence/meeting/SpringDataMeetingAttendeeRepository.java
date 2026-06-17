package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.meeting.AttendeeRole;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataMeetingAttendeeRepository
        extends JpaRepository<MeetingAttendeeEntity, UUID> {

    List<MeetingAttendeeEntity> findByMeetingId(UUID meetingId);

    List<MeetingAttendeeEntity> findByUserId(UUID userId);

    /** 회의록 검토자 연동용: 회의의 특정 역할(REVIEWER) 참석자 조회. 회의당 검토자는 최대 1명이다. */
    Optional<MeetingAttendeeEntity> findByMeetingIdAndRole(UUID meetingId, AttendeeRole role);

    /**
     * 회의의 모든 참석자를 삭제한다. 회의 수정 시 참석자 전체 교체(삭제 후 재삽입)에 사용된다.
     *
     * <p>{@code flushAutomatically=true}로 보류 중인 변경을 먼저 flush한 뒤 즉시 bulk delete를 실행한다. 이렇게 해야 같은
     * 트랜잭션의 후속 재삽입(saveAll)이 (meeting_id, user_id) 유니크 제약과 충돌하지 않는다(Hibernate 기본 flush 순서상 INSERT가
     * DELETE보다 먼저 실행되는 문제 회피). {@code clearAutomatically=true}로 영속성 컨텍스트의 잔여 엔티티도 정리한다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MeetingAttendeeEntity a where a.meetingId = :meetingId")
    void deleteByMeetingId(@Param("meetingId") UUID meetingId);
}
