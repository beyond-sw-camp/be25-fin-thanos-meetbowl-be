package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 건물 JPA Entity다. {@code building} 테이블과 1:1로 매핑된다. 사이트는 raw UUID로 참조한다. */
@Entity
@Table(
        name = "building",
        indexes = {@Index(name = "idx_building_site", columnList = "site_id")})
public class BuildingEntity extends BaseEntity {

    /** 소속 사이트(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID siteId;

    /** 건물명. */
    @Column(nullable = false, length = 100)
    private String name;

    protected BuildingEntity() {}

    private BuildingEntity(UUID siteId, String name) {
        this.siteId = siteId;
        this.name = name;
    }

    static BuildingEntity from(Building building) {
        BuildingEntity entity = new BuildingEntity(building.siteId(), building.name());
        entity.setId(building.id());
        return entity;
    }

    Building toDomain() {
        return Building.of(getId(), siteId, name);
    }
}
