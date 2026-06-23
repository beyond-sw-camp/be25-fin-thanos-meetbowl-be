package com.meetbowl.domain.minutes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의록 즐겨찾기 도메인이 저장 기술을 모르고 조회·저장·삭제를 요청하기 위한 경계다. */
public interface MinutesFavoriteRepositoryPort {

    MinutesFavorite save(MinutesFavorite favorite);

    Optional<MinutesFavorite> findByUserIdAndMinutesId(UUID userId, UUID minutesId);

    List<MinutesFavorite> findByUserId(UUID userId);

    void deleteByUserIdAndMinutesId(UUID userId, UUID minutesId);
}
