package com.meetbowl.infrastructure.persistence.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 사용자 계정 정보를 저장하는 엔티티다.
 * 인증/권한(Role), 상태, 조직 소속(소속/부서/팀) 및 직급(Position)을 ID로 참조한다.
 */
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    /** 로그인에 사용하는 고유 아이디. */
    @Column(nullable = false, unique = true, length = 100)
    private String loginId;

    /** 비밀번호 해시(평문 저장 금지). */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /** 사용자 실명. */
    @Column(nullable = false, length = 100)
    private String name;

    /** 사용자 이메일(고유). */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** 사용자 역할(권한 그룹). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** 사용자 상태(활성/잠금/탈퇴 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** 소속(Affiliate) ID(UUID). 조직 최상위 단위. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID affiliateId;

    /** 부서(Department) ID(UUID). 소속 하위 조직. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID departmentId;

    /** 직급/직위(Position) ID(UUID). 사용자의 직급을 나타냄. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID positionId;

    /** 팀(Team) ID(UUID). 부서 하위 조직 단위. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID teamId;

    /** 최초 로그인 시 비밀번호 변경 필요 여부. */
    @Column(nullable = false)
    private boolean initialPasswordChangeRequired;

    /** 계정 활성 시작 시각(UTC). null이면 제한 없음. */
    private Instant activeFrom;

    /** 계정 활성 종료 시각(UTC). null이면 제한 없음. */
    private Instant activeUntil;

    protected UserEntity() {}

    private UserEntity(User user) {
        this.loginId = user.loginId();
        this.passwordHash = user.passwordHash();
        this.name = user.name();
        this.email = user.email();
        this.role = user.role();
        this.status = user.status();
        this.affiliateId = user.affiliateId();
        this.departmentId = user.departmentId();
        this.positionId = user.positionId();
        this.teamId = user.teamId();
        this.initialPasswordChangeRequired = user.initialPasswordChangeRequired();
        this.activeFrom = user.activeFrom();
        this.activeUntil = user.activeUntil();
    }

    static UserEntity from(User user) {
        UserEntity entity = new UserEntity(user);
        entity.setId(user.id());
        return entity;
    }

    User toDomain() {
        return User.of(
                getId(),
                loginId,
                passwordHash,
                name,
                email,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                getCreatedAt(),
                getUpdatedAt());
    }
}
