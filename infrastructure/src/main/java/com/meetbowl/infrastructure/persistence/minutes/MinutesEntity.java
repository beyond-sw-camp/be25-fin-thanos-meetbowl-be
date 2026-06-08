package com.meetbowl.infrastructure.persistence.minutes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** minutes 테이블과 매핑되는 JPA Entity다. infrastructure 밖으로 노출하지 않고 Minutes 도메인으로 변환해서 사용한다. */
@Entity
@Table(
        name = "minutes",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_minutes_meeting_id", columnNames = "meeting_id"))
public class MinutesEntity extends BaseEntity {

    @Column(name = "meeting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(name = "organization_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(name = "reviewer_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reviewerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MinutesStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 100)
    private String promptVersion;

    @Column(columnDefinition = "BINARY(16)")
    private UUID approvedByUserId;

    private Instant approvedAt;

    private Instant sharedAt;

    private Instant deletionScheduledAt;

    /** JPA가 프록시와 조회 객체를 만들 때 사용하는 기본 생성자다. */
    protected MinutesEntity() {}

    /** Entity 생성 경로를 from(Minutes)로 제한하기 위해 private 생성자로 둔다. */
    private MinutesEntity(
            UUID meetingId,
            UUID organizationId,
            UUID reviewerUserId,
            MinutesStatus status,
            String summary,
            String model,
            String promptVersion,
            UUID approvedByUserId,
            Instant approvedAt,
            Instant sharedAt,
            Instant deletionScheduledAt) {
        this.meetingId = meetingId;
        this.organizationId = organizationId;
        this.reviewerUserId = reviewerUserId;
        this.status = status;
        this.summary = summary;
        this.model = model;
        this.promptVersion = promptVersion;
        this.approvedByUserId = approvedByUserId;
        this.approvedAt = approvedAt;
        this.sharedAt = sharedAt;
        this.deletionScheduledAt = deletionScheduledAt;
    }

    /** 도메인 모델을 DB 저장용 Entity로 변환한다. */
    static MinutesEntity from(Minutes minutes) {
        MinutesEntity entity =
                new MinutesEntity(
                        minutes.meetingId(),
                        minutes.organizationId(),
                        minutes.reviewerUserId(),
                        minutes.status(),
                        minutes.summary(),
                        minutes.model(),
                        minutes.promptVersion(),
                        minutes.approvedByUserId(),
                        minutes.approvedAt(),
                        minutes.sharedAt(),
                        minutes.deletionScheduledAt());
        entity.setId(minutes.id());
        return entity;
    }

    /** DB에서 조회한 Entity를 application/domain 계층으로 넘길 도메인 모델로 복원한다. */
    Minutes toDomain() {
        return Minutes.of(
                getId(),
                meetingId,
                organizationId,
                reviewerUserId,
                status,
                summary,
                model,
                promptVersion,
                approvedByUserId,
                approvedAt,
                sharedAt,
                deletionScheduledAt);
    }
}
