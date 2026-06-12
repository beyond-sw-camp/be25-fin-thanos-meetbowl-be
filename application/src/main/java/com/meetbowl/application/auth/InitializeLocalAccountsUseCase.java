package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

/** 로컬 실행 환경에서 Swagger로 인증·메일 흐름을 확인할 수 있도록 최소 계정을 준비한다. */
@Service
public class InitializeLocalAccountsUseCase {

    private static final String LOCAL_PASSWORD = "1234";

    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public InitializeLocalAccountsUseCase(
            AffiliateRepositoryPort affiliateRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder) {
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void execute() {
        var admin = userRepositoryPort.findByLoginId("admin");
        var user1 = userRepositoryPort.findByLoginId("user1");
        var user2 = userRepositoryPort.findByLoginId("user2");
        if (admin.isPresent() && user1.isPresent() && user2.isPresent()) {
            return;
        }

        UUID affiliateId =
                admin.map(User::affiliateId)
                        .or(() -> user1.map(User::affiliateId))
                        .or(() -> user2.map(User::affiliateId))
                        .filter(java.util.Objects::nonNull)
                        .orElseGet(this::createLocalAffiliate);
        String passwordHash = passwordEncoder.encode(LOCAL_PASSWORD);
        if (admin.isEmpty()) {
            userRepositoryPort.save(
                    localUser("admin", "로컬 관리자", UserRole.ADMIN, affiliateId, passwordHash));
        }
        if (user1.isEmpty()) {
            userRepositoryPort.save(
                    localUser("user1", "로컬 사용자 1", UserRole.USER, affiliateId, passwordHash));
        }
        if (user2.isEmpty()) {
            userRepositoryPort.save(
                    localUser("user2", "로컬 사용자 2", UserRole.USER, affiliateId, passwordHash));
        }
    }

    private UUID createLocalAffiliate() {
        Affiliate affiliate =
                affiliateRepositoryPort.save(
                        new Affiliate(
                                null, "로컬 테스트 조직", "LOCAL", ReferenceStatus.ACTIVE, 1, null, null));
        return affiliate.id();
    }

    private User localUser(
            String loginId, String name, UserRole role, UUID affiliateId, String passwordHash) {
        Instant now = Instant.now();
        return User.of(
                null,
                loginId,
                passwordHash,
                name,
                loginId + "@local.meetbowl",
                role,
                UserStatus.ACTIVE,
                affiliateId,
                null,
                null,
                null,
                false,
                null,
                null,
                now,
                now);
    }
}
