package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 커뮤니티 익명 표시명 매핑 JPA Entity다. {@code community_alias} 테이블과 1:1로 매핑된다. user_id·alias_no 각각 유니크로 사용자당
 * 1개·번호 중복 없음을 보장한다.
 */
@Entity
@Table(
        name = "community_alias",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_community_alias_user", columnNames = "user_id"),
            @UniqueConstraint(name = "uk_community_alias_no", columnNames = "alias_no")
        })
public class CommunityAliasEntity extends BaseEntity {

    /** 대상 사용자(FK, 유니크). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 전역 익명 번호(유니크). */
    @Column(nullable = false)
    private int aliasNo;

    protected CommunityAliasEntity() {}

    private CommunityAliasEntity(UUID userId, int aliasNo) {
        this.userId = userId;
        this.aliasNo = aliasNo;
    }

    static CommunityAliasEntity from(CommunityAlias alias) {
        CommunityAliasEntity entity = new CommunityAliasEntity(alias.userId(), alias.aliasNo());
        entity.setId(alias.id());
        return entity;
    }

    CommunityAlias toDomain() {
        return CommunityAlias.of(getId(), userId, aliasNo);
    }
}
