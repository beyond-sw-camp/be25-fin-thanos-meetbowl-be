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

/**
 * 부서(Department) 정보를 저장하는 엔티티다.
 * 소속(Affiliate) 하위에 위치하며, 상위 부서를 가질 수 있다.
 */
@Entity
@Table(name = "departments")
public class DepartmentEntity extends BaseEntity {
    /** 소속(Affiliate) ID(UUID). 연관은 ID로만 참조한다. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID affiliateId;

    /** 상위 부서 ID(UUID). 최상위면 null. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID parentDepartmentId;

    /** 부서명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 부서 코드(내부 식별용). */
    @Column(length = 50)
    private String code;

    /** 참조 상태(사용/중지 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    /** 정렬 순서(작을수록 상위). */
    private Integer sortOrder;

    protected DepartmentEntity() {}

    private DepartmentEntity(Department department) {
        affiliateId = department.affiliateId();
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
                affiliateId,
                parentDepartmentId,
                name,
                code,
                status,
                sortOrder,
                getCreatedAt(),
                getUpdatedAt());
    }
}
