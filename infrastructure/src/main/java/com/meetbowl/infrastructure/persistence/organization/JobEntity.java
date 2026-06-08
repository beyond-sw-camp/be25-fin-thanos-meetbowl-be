package com.meetbowl.infrastructure.persistence.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Job;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "jobs")
public class JobEntity extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    private Integer sortOrder;

    protected JobEntity() {}

    private JobEntity(Job job) {
        name = job.name();
        code = job.code();
        status = job.status();
        sortOrder = job.sortOrder();
    }

    static JobEntity from(Job job) {
        JobEntity entity = new JobEntity(job);
        entity.setId(job.id());
        return entity;
    }

    Job toDomain() {
        return new Job(getId(), name, code, status, sortOrder, getCreatedAt(), getUpdatedAt());
    }
}
