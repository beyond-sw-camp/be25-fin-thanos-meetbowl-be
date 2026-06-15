package com.meetbowl.infrastructure.persistence.user;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 사용자 개인 설정을 저장하는 엔티티다. 회의 알림, 회의록 검토 알림, 자동 백업 설정을 관리한다. */
@Entity
@Table(name = "user_settings")
public class UserSettingEntity extends BaseEntity {
    /** 설정 소유자 사용자 ID(UUID, 고유)다. */
    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 회의 시작 몇 분 전에 알림할지 저장한다. */
    @Column(nullable = false)
    private int meetingReminderMinutesBefore;

    /** 회의록을 아직 검토하지 않았을 때 추가 알림을 보내는 주기(분)다. */
    @Column(nullable = false)
    private int minutesReviewReminderMinutes;

    /** 자동 백업 사용 여부다. */
    @Column(nullable = false)
    private boolean autoBackupEnabled;

    protected UserSettingEntity() {}

    private UserSettingEntity(UserSetting userSetting) {
        userId = userSetting.userId();
        meetingReminderMinutesBefore = userSetting.meetingReminderMinutesBefore();
        // 도메인에서 검증된 분 단위 값을 그대로 영속화한다.
        minutesReviewReminderMinutes = userSetting.minutesReviewReminderMinutes();
        autoBackupEnabled = userSetting.autoBackupEnabled();
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
                minutesReviewReminderMinutes,
                autoBackupEnabled,
                getCreatedAt(),
                getUpdatedAt());
    }
}
