package com.meetbowl.infrastructure.persistence.common;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** JPA Entity가 공통으로 상속하는 기반 클래스다. 주요 도메인 ID는 UUID를 기본으로 하고, 저장/수정 시각은 UTC Instant로 다룬다. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    protected void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 도메인 모델에서 엔티티를 재구성(merge용)할 때 기존 생성 시각을 보존하기 위한 설정자다. 새로 저장하는 엔티티에서는 호출하지 않아야 한다(그 경우 감사 리스너가
     * 채운다). 컬럼이 updatable=false라 이 값이 UPDATE로 다시 쓰이지는 않지만, merge 후 반환 객체가 생성 시각을 잃지 않도록 채운다.
     */
    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
