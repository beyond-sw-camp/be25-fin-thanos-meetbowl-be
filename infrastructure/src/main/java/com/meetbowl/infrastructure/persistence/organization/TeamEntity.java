package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 팀(Team) 정보를 저장하는 엔티티다.
 * 특정 부서(Department) 하위에 속하는 조직 단위를 나타낸다.
 */
@Entity
@Table(name = "teams")
public class TeamEntity extends BaseEntity {

    /** 소속 부서 ID(UUID). 연관은 ID로만 참조한다. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID departmentId;

    /** 팀명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 팀 코드(내부 식별용). */
    @Column(length = 50)
    private String code;

    /** 참조 상태(사용/중지 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    /** 정렬 순서(작을수록 상위). */
    private Integer sortOrder;

    protected TeamEntity() {}

    private TeamEntity(Team team) {
        this.departmentId = team.departmentId();
        this.name = team.name();
        this.code = team.code();
        this.status = team.status();
        this.sortOrder = team.sortOrder();
    }

    static TeamEntity from(Team team) {
        TeamEntity entity = new TeamEntity(team);
        entity.setId(team.id());
        return entity;
    }

    Team toDomain() {
        return new Team(
                getId(),
                departmentId,
                name,
                code,
                status,
                sortOrder,
                getCreatedAt(),
                getUpdatedAt());
    }
}
