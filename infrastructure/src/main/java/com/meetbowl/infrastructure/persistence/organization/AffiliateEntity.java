package com.meetbowl.infrastructure.persistence.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 소속(회사/기관 등 최상위 조직) 정보를 저장하는 엔티티다. 하위에 부서(Department)와 팀(Team)이 속한다. */
@Entity
@Table(name = "affiliates")
public class AffiliateEntity extends BaseEntity {

    /** 소속명(회사/기관명). */
    @Column(nullable = false, length = 100)
    private String name;

    /** 소속 코드(내부 식별용). */
    @Column(length = 50)
    private String code;

    /** 참조 상태(사용/중지 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

    /** 정렬 순서(작을수록 상위). */
    private Integer sortOrder;

    protected AffiliateEntity() {}

    private AffiliateEntity(Affiliate organization) {
        this.name = organization.name();
        this.code = organization.code();
        this.status = organization.status();
        this.sortOrder = organization.sortOrder();
    }

    static AffiliateEntity from(Affiliate organization) {
        AffiliateEntity entity = new AffiliateEntity(organization);
        entity.setId(organization.id());
        return entity;
    }

    Affiliate toDomain() {
        return new Affiliate(
                getId(), name, code, status, sortOrder, getCreatedAt(), getUpdatedAt());
    }
}
