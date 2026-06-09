package com.meetbowl.infrastructure.persistence.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 직급/직위(Position) 정보를 저장하는 엔티티다.
 * 사용자(User)의 직급을 표현할 때 참조된다.
 */
@Entity
@Table(name = "positions")
public class PositionEntity extends BaseEntity {
    /** 직급명(예: 사원, 대리, 과장). */
    @Column(nullable = false, length = 100)
    private String name;

    /** 직급 코드(내부 식별용). */
    @Column(length = 50)
    private String code;

    /** 참조 상태(사용/중지 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    /** 정렬 순서(작을수록 상위). */
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
