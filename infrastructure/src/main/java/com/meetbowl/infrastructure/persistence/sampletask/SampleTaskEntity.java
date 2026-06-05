package com.meetbowl.infrastructure.persistence.sampletask;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 샘플 JPA Entity다. Entity는 infrastructure 내부 모델이며 API/Application/Domain 계층으로 노출하지 않는다. */
@Entity
@Table(name = "sample_tasks")
public class SampleTaskEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Instant taskCreatedAt;

    protected SampleTaskEntity() {}

    private SampleTaskEntity(UUID ownerUserId, String title, Instant taskCreatedAt) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.taskCreatedAt = taskCreatedAt;
    }

    static SampleTaskEntity from(SampleTask sampleTask) {
        SampleTaskEntity entity =
                new SampleTaskEntity(
                        sampleTask.ownerUserId(), sampleTask.title(), sampleTask.createdAt());
        entity.setId(sampleTask.id());
        return entity;
    }

    SampleTask toDomain() {
        return SampleTask.of(getId(), ownerUserId, title, taskCreatedAt);
    }
}
