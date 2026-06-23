package com.meetbowl.application.minutes;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 회의록 즐겨찾기를 해제한다. 해제 자체는 멱등하게 처리한다. */
@Service
public class RemoveMinutesFavoriteUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MinutesFavoriteRepositoryPort favoriteRepositoryPort;

    public RemoveMinutesFavoriteUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MinutesFavoriteRepositoryPort favoriteRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.favoriteRepositoryPort = favoriteRepositoryPort;
    }

    @Transactional
    public void execute(UUID actorUserId, UUID actorOrganizationId, UUID minutesId) {
        // 해제 요청도 먼저 회의록 조직 경계를 확인해 다른 조직 회의록 ID로 사용자 상태를 변경하지 못하게 한다.
        Minutes minutes =
                minutesRepositoryPort
                        .findById(minutesId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
        MinutesAccessValidator.ensureSameOrganization(minutes, actorOrganizationId);
        favoriteRepositoryPort.deleteByUserIdAndMinutesId(actorUserId, minutes.id());
    }
}
