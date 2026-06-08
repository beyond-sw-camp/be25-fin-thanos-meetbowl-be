package com.meetbowl.infrastructure.persistence.user;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "user_settings")
public class UserSettingEntity extends BaseEntity {
    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false)
    private int meetingReminderMinutesBefore;

    @Column(nullable = false)
    private boolean autoBackupEnabled;

    private LocalTime autoBackupTime;

    protected UserSettingEntity() {}

    private UserSettingEntity(UserSetting userSetting) {
        userId = userSetting.userId();
        meetingReminderMinutesBefore = userSetting.meetingReminderMinutesBefore();
        autoBackupEnabled = userSetting.autoBackupEnabled();
        autoBackupTime = userSetting.autoBackupTime();
    }

    static UserSettingEntity from(UserSetting userSetting) {
        UserSettingEntity entity = new UserSettingEntity(userSetting);
        entity.setId(userSetting.id());
        return entity;
    }

    UserSetting toDomain() {
        return new UserSetting(
                getId(),
                userId,
                meetingReminderMinutesBefore,
                autoBackupEnabled,
                autoBackupTime,
                getCreatedAt(),
                getUpdatedAt());
    }
}
