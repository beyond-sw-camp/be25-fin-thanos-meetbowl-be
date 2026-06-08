package com.meetbowl.infrastructure.persistence.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "positions")
public class PositionEntity extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    private Integer sortOrder;

    protected PositionEntity() {}

    private PositionEntity(Position position) {
        name = position.name();
        code = position.code();
        status = position.status();
        sortOrder = position.sortOrder();
    }

    static PositionEntity from(Position position) {
        PositionEntity entity = new PositionEntity(position);
        entity.setId(position.id());
        return entity;
    }

    Position toDomain() {
        return new Position(getId(), name, code, status, sortOrder, getCreatedAt(), getUpdatedAt());
    }
}
