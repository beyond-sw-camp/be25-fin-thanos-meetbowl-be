package com.meetbowl.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

class MySettingsUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC);

    private FakeUserSettingRepository userSettingRepository;
    private MySettingsUseCase useCase;

    @BeforeEach
    void setUp() {
        userSettingRepository = new FakeUserSettingRepository();
        useCase = new MySettingsUseCase(userSettingRepository, FIXED_CLOCK);
    }

    @Test
    void getSettingsReturnsDomainDefaultsWhenSettingDoesNotExist() {
        MySettingsResult result = useCase.get(USER_ID);

        assertEquals(
                UserSetting.DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                result.meetingStartReminderMinutes());
        assertEquals(false, result.autoBackupEnabled());
        assertEquals(UserSetting.DEFAULT_AUTO_BACKUP_TIME, result.autoBackupTime());
    }

    @Test
    void updateSettingsSuccessCreatesSettingForCurrentUser() {
        MySettingsResult result =
                useCase.update(
                        new MySettingsUseCase.UpdateMySettingsCommand(
                                USER_ID, 30, true, LocalTime.of(20, 0)));

        assertEquals(30, result.meetingStartReminderMinutes());
        assertEquals(true, result.autoBackupEnabled());
        assertEquals(LocalTime.of(20, 0), result.autoBackupTime());
        assertEquals(USER_ID, userSettingRepository.findByUserId(USER_ID).orElseThrow().userId());
    }

    @Test
    void updateSettingsFailsWhenReminderMinutesIsNegative() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new MySettingsUseCase.UpdateMySettingsCommand(
                                                USER_ID, -1, false, LocalTime.of(18, 0))));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void updateSettingsFailsWhenAutoBackupEnabledWithoutTime() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new MySettingsUseCase.UpdateMySettingsCommand(
                                                USER_ID, 10, true, null)));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    private static final class FakeUserSettingRepository implements UserSettingRepositoryPort {
        private final Map<UUID, UserSetting> settings = new ConcurrentHashMap<>();

        @Override
        public UserSetting save(UserSetting userSetting) {
            UserSetting saved =
                    new UserSetting(
                            userSetting.id() == null ? UUID.randomUUID() : userSetting.id(),
                            userSetting.userId(),
                            userSetting.meetingReminderMinutesBefore(),
                            userSetting.autoBackupEnabled(),
                            userSetting.autoBackupTime(),
                            userSetting.createdAt(),
                            userSetting.updatedAt());
            settings.put(saved.userId(), saved);
            return saved;
        }

        @Override
        public Optional<UserSetting> findByUserId(UUID userId) {
            return Optional.ofNullable(settings.get(userId));
        }
    }
}
