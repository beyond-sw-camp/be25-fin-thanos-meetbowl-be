package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingRepositoryPort {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(UUID id);

    /** 내가 주최한 회의 조회용. */
    List<Meeting> findByHostUserId(UUID hostUserId);

    void deleteById(UUID id);
}
