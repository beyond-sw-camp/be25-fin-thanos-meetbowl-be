package com.meetbowl.infrastructure.persistence.meetingroom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 사이트(거점) JPA Entity다. {@code site} 테이블과 1:1로 매핑된다. */
@Entity
@Table(name = "sites")
public class SiteEntity extends BaseEntity {

    /** 사이트명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 주소(nullable). */
    @Column(length = 300)
    private String address;

    protected SiteEntity() {}

    private SiteEntity(String name, String address) {
        this.name = name;
        this.address = address;
    }

    static SiteEntity from(Site site) {
        SiteEntity entity = new SiteEntity(site.name(), site.address());
        entity.setId(site.id());
        return entity;
    }

    Site toDomain() {
        return Site.of(getId(), name, address);
    }
}
