package com.meetbowl.infrastructure.persistence.user;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 사용자 개인 설정을 저장하는 엔티티다.
 * 회의 알림, 자동 백업 설정 등 사용자별 환경을 관리한다.
 */
@Entity
@Table(name = "user_settings")
public class UserSettingEntity extends BaseEntity {
    /** 설정 소유자 사용자 ID(UUID, 고유). */
    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 회의 시작 몇 분 전에 알림할지(분). */
    @Column(nullable = false)
    private int meetingReminderMinutesBefore;

    /** 자동 백업 사용 여부. */
    @Column(nullable = false)
    private boolean autoBackupEnabled;

    /** 자동 백업 실행 시각(서버 로컬 시간이 아닌 LocalTime). */
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
