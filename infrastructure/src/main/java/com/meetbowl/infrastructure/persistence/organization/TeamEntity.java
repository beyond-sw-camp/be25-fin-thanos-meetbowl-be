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

@Entity
@Table(name = "teams")
public class TeamEntity extends BaseEntity {

    @Column(columnDefinition = "BINARY(16)")
    private UUID departmentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

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
