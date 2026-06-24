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

/** ?ъ슜??怨꾩젙 ?뺣낫瑜???ν븯???뷀떚?곕떎. ?몄쬆/沅뚰븳(Role), ?곹깭, 議곗쭅 ?뚯냽(?뚯냽/遺???) 諛?吏곴툒(Position)??ID濡?李몄“?쒕떎. */
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    /** 濡쒓렇?몄뿉 ?ъ슜?섎뒗 怨좎쑀 ?꾩씠?? */
    @Column(nullable = false, unique = true, length = 100)
    private String loginId;

    /** 鍮꾨?踰덊샇 ?댁떆(?됰Ц ???湲덉?). */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /** ?ъ슜???ㅻ챸. */
    @Column(nullable = false, length = 100)
    private String name;

    /** ?ъ슜???대찓??怨좎쑀). */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** ?ъ슜????븷(沅뚰븳 洹몃９). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** ?ъ슜???곹깭(?쒖꽦/?좉툑/?덊눜 ??. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** ?뚯냽(Affiliate) ID(UUID). 議곗쭅 理쒖긽???⑥쐞. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID affiliateId;

    /** 遺??Department) ID(UUID). ?뚯냽 ?섏쐞 議곗쭅. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID departmentId;

    /** 吏곴툒/吏곸쐞(Position) ID(UUID). ?ъ슜?먯쓽 吏곴툒???섑??? */
    @Column(columnDefinition = "BINARY(16)")
    private UUID positionId;

    /** ?(Team) ID(UUID). 遺???섏쐞 議곗쭅 ?⑥쐞. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID teamId;

    /** 理쒖큹 濡쒓렇????鍮꾨?踰덊샇 蹂寃??꾩슂 ?щ?. */
    @Column(nullable = false)
    private boolean initialPasswordChangeRequired;

    /** 怨꾩젙 ?쒖꽦 ?쒖옉 ?쒓컖(UTC). null?대㈃ ?쒗븳 ?놁쓬. */
    private Instant activeFrom;

    /** 怨꾩젙 ?쒖꽦 醫낅즺 ?쒓컖(UTC). null?대㈃ ?쒗븳 ?놁쓬. */
    private Instant activeUntil;

    /** 삭제된 회원은 연관 데이터 보존을 위해 물리 삭제하지 않고 deletedAt만 기록한다. */
    private Instant deletedAt;

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
        this.deletedAt = user.deletedAt();
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
                deletedAt,
                getCreatedAt(),
                getUpdatedAt());
    }
}
