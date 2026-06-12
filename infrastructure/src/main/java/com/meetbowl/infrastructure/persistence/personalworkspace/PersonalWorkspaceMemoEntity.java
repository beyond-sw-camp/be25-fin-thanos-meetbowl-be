package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** {@link PersonalWorkspaceMemo} 애그리거트를 관계형 스키마에 저장하기 위한 영속 엔티티다. 도메인↔엔티티 변환을 이 경계 안에 둔다. */
@Entity
@Table(name = "personal_workspace_memos")
public class PersonalWorkspaceMemoEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false)
    private Instant memoCreatedAt;

    @Column(nullable = false)
    private Instant memoUpdatedAt;

    protected PersonalWorkspaceMemoEntity() {}

    private PersonalWorkspaceMemoEntity(
            UUID ownerUserId,
            String title,
            String content,
            Instant memoCreatedAt,
            Instant memoUpdatedAt) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.content = content;
        this.memoCreatedAt = memoCreatedAt;
        this.memoUpdatedAt = memoUpdatedAt;
    }

    static PersonalWorkspaceMemoEntity from(PersonalWorkspaceMemo memo) {
        PersonalWorkspaceMemoEntity entity =
                new PersonalWorkspaceMemoEntity(
                        memo.ownerUserId(),
                        memo.title(),
                        memo.content(),
                        memo.memoCreatedAt(),
                        memo.memoUpdatedAt());
        entity.setId(memo.id());
        return entity;
    }

    PersonalWorkspaceMemo toDomain() {
        return PersonalWorkspaceMemo.of(
                getId(), ownerUserId, title, content, memoCreatedAt, memoUpdatedAt);
    }
}
