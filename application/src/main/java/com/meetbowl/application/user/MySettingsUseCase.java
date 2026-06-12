package com.meetbowl.application.user;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
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
                        () ->
                                toResult(
                                        UserSetting.createDefault(
                                                currentUserId, Instant.now(clock))));
    }

    @Transactional
    public MySettingsResult update(UpdateMySettingsCommand command) {
        Instant now = Instant.now(clock);
        UserSetting current =
                userSettingRepositoryPort
                        .findByUserId(command.userId())
                        .orElseGet(() -> UserSetting.createDefault(command.userId(), now));

        UserSetting updated =
                new UserSetting(
                        current.id(),
                        command.userId(),
                        command.meetingStartReminderMinutes(),
                        command.autoBackupEnabled(),
                        command.autoBackupTime(),
                        current.createdAt(),
                        now);
        return toResult(userSettingRepositoryPort.save(updated));
    }

    private MySettingsResult toResult(UserSetting setting) {
        return new MySettingsResult(
                setting.meetingReminderMinutesBefore(),
                setting.autoBackupEnabled(),
                setting.autoBackupTime());
    }

    public record UpdateMySettingsCommand(
            UUID userId,
            int meetingStartReminderMinutes,
            boolean autoBackupEnabled,
            LocalTime autoBackupTime) {}
}
