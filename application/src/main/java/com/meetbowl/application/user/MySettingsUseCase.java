package com.meetbowl.application.user;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

@Service
public class MySettingsUseCase {

    private final UserSettingRepositoryPort userSettingRepositoryPort;
    private final Clock clock;

    public MySettingsUseCase(UserSettingRepositoryPort userSettingRepositoryPort, Clock clock) {
        this.userSettingRepositoryPort = userSettingRepositoryPort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MySettingsResult get(UUID currentUserId) {
        return userSettingRepositoryPort
                .findByUserId(currentUserId)
                .map(this::toResult)
                .orElseGet(
                        () -> {
                            // 설정 레코드가 아직 없으면 화면이 바로 렌더링되도록 기본 설정을 내려준다.
                            return toResult(
                                    UserSetting.createDefault(currentUserId, Instant.now(clock)));
                        });
    }

    @Transactional
    public MySettingsResult update(UpdateMySettingsCommand command) {
        Instant now = Instant.now(clock);
        UserSetting current =
                userSettingRepositoryPort
                        .findByUserId(command.userId())
                        // 첫 저장 요청이어도 기존 생성 플로우를 재사용해 기본 설정을 기준점으로 삼는다.
                        .orElseGet(() -> UserSetting.createDefault(command.userId(), now));

        UserSetting updated =
                new UserSetting(
                        current.id(),
                        command.userId(),
                        command.meetingStartReminderMinutes(),
                        // 회의록 미검토 알림 주기는 도메인에서 허용값(60/120/180/240)만 저장한다.
                        command.minutesReviewReminderMinutes(),
                        command.autoBackupEnabled(),
                        current.createdAt(),
                        now);
        return toResult(userSettingRepositoryPort.save(updated));
    }

    private MySettingsResult toResult(UserSetting setting) {
        return new MySettingsResult(
                // API 응답 필드명은 기존 프론트 계약인 meetingStartReminderMinutes를 유지한다.
                setting.meetingReminderMinutesBefore(),
                setting.minutesReviewReminderMinutes(),
                setting.autoBackupEnabled());
    }

    public record UpdateMySettingsCommand(
            UUID userId,
            int meetingStartReminderMinutes,
            int minutesReviewReminderMinutes,
            boolean autoBackupEnabled) {}
}
