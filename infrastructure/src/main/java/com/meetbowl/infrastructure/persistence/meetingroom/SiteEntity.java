package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 사이트(거점) JPA Entity다. {@code site} 테이블과 1:1로 매핑된다. */
@Entity
@Table(
        name = "site",
        indexes = {@Index(name = "idx_site_affiliate", columnList = "affiliate_id")})
public class SiteEntity extends BaseEntity {

    /** 소속 계열사(FK). */
    @Column(columnDefinition = "BINARY(16)")
    private UUID affiliateId;

    /** 사이트명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 주소(nullable). */
    @Column(length = 300)
    private String address;

    protected SiteEntity() {}

    private SiteEntity(UUID affiliateId, String name, String address) {
        this.affiliateId = affiliateId;
        this.name = name;
        this.address = address;
    }

    static SiteEntity from(Site site) {
        SiteEntity entity = new SiteEntity(site.affiliateId(), site.name(), site.address());
        entity.setId(site.id());
        return entity;
    }

    Site toDomain() {
        return Site.of(getId(), affiliateId, name, address);
    }
}
