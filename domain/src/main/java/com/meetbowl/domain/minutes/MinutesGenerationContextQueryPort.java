package com.meetbowl.domain.minutes;

import java.util.Optional;
import java.util.UUID;

/** 회의록 생성용 내부 Context를 영속 데이터에서 조립하는 조회 Port다. */
public interface MinutesGenerationContextQueryPort {

    Optional<MinutesGenerationContext> findByMeetingId(UUID meetingId);
}
