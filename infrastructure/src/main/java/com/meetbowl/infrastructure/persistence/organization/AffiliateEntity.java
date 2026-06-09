package com.meetbowl.infrastructure.persistence.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "organizations")
public class AffiliateEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceStatus status;

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
