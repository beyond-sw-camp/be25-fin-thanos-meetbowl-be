package com.meetbowl.infrastructure.persistence.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestStatus;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.AuthUserOrgJpaConfig;

@SpringBootTest(classes = JpaPasswordResetRequestRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:password-reset-request-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaPasswordResetRequestRepositoryAdapterTest {

    @Autowired private JpaPasswordResetRequestRepositoryAdapter adapter;

    @Test
    void findAllByStatusAndCountPending() {
        adapter.save(request(UUID.randomUUID(), PasswordResetRequestStatus.REJECTED, 0));
        PasswordResetRequest newestPending =
                adapter.save(request(UUID.randomUUID(), PasswordResetRequestStatus.PENDING, 10));
        adapter.save(request(UUID.randomUUID(), PasswordResetRequestStatus.PENDING, 5));

        assertThat(adapter.countByStatus(PasswordResetRequestStatus.PENDING)).isEqualTo(2);
        assertThat(adapter.findAllByStatus(PasswordResetRequestStatus.PENDING))
                .hasSize(2)
                .first()
                .extracting(PasswordResetRequest::id)
                .isEqualTo(newestPending.id());
    }

    private PasswordResetRequest request(UUID id, PasswordResetRequestStatus status, long plusSeconds) {
        Instant base = Instant.parse("2026-06-23T00:00:00Z").plusSeconds(plusSeconds);
        return new PasswordResetRequest(
                id,
                UUID.randomUUID(),
                "User One",
                "user1",
                "user1@example.com",
                status,
                base,
                status == PasswordResetRequestStatus.PENDING ? null : base.plusSeconds(60),
                status == PasswordResetRequestStatus.PENDING ? null : UUID.randomUUID(),
                base,
                base);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        AuthUserOrgJpaConfig.class,
        JpaPasswordResetRequestRepositoryAdapter.class
    })
    static class TestApplication {}
}
