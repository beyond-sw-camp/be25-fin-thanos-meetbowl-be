package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "departments")
public class DepartmentEntity extends BaseEntity {
    @Column(columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID parentDepartmentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    private Integer sortOrder;

    protected DepartmentEntity() {}

    private DepartmentEntity(Department department) {
        organizationId = department.organizationId();
        parentDepartmentId = department.parentDepartmentId();
        name = department.name();
        code = department.code();
        status = department.status();
        sortOrder = department.sortOrder();
    }

    static DepartmentEntity from(Department department) {
        DepartmentEntity entity = new DepartmentEntity(department);
        entity.setId(department.id());
        return entity;
    }

    Department toDomain() {
        return new Department(
                getId(),
                organizationId,
                parentDepartmentId,
                name,
                code,
                status,
                sortOrder,
                getCreatedAt(),
                getUpdatedAt());
    }
}
