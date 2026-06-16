package com.meetbowl.application.minutes;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavorite;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 개인 워크스페이스에서 다시 볼 회의록 즐겨찾기를 등록한다. */
@Service
public class AddMinutesFavoriteUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MinutesFavoriteRepositoryPort favoriteRepositoryPort;

    public AddMinutesFavoriteUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MinutesFavoriteRepositoryPort favoriteRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.favoriteRepositoryPort = favoriteRepositoryPort;
    }

    @Transactional
    public void execute(UUID actorUserId, UUID actorOrganizationId, UUID minutesId) {
        Minutes minutes = findReadableMinutes(actorOrganizationId, minutesId);
        // 같은 회의록을 여러 번 눌러도 최종 상태는 즐겨찾기 1건이므로 멱등하게 성공 처리한다.
        if (favoriteRepositoryPort
                .findByUserIdAndMinutesId(actorUserId, minutes.id())
                .isPresent()) {
            return;
        }
        favoriteRepositoryPort.save(
                MinutesFavorite.create(actorUserId, minutes.id(), Instant.now()));
    }

    private Minutes findReadableMinutes(UUID actorOrganizationId, UUID minutesId) {
        Minutes minutes =
                minutesRepositoryPort
                        .findById(minutesId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
        MinutesAccessValidator.ensureSameOrganization(minutes, actorOrganizationId);
        return minutes;
    }
}
