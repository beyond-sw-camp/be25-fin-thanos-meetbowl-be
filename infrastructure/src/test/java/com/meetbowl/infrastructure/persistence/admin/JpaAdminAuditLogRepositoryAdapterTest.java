package com.meetbowl.infrastructure.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogSearchCondition;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.AuthUserOrgJpaConfig;

@ActiveProfiles("admin-audit-jpa")
@SpringBootTest(classes = JpaAdminAuditLogRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:admin-audit-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaAdminAuditLogRepositoryAdapterTest {

    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-12T00:00:00Z");

    @Autowired private JpaAdminAuditLogRepositoryAdapter adapter;

    @Test
    void findPageFiltersMailRetentionPolicyUpdateWithoutMixingOtherUpdateLogs() {
        adapter.save(log("MAIL_RETENTION_POLICY", POLICY_ID, "MAIL_RETENTION_POLICY", "UPDATE"));
        adapter.save(log("USER", USER_ID, "USER_MANAGEMENT", "UPDATE"));

        var result =
                adapter.findPage(
                        new AdminAuditLogSearchCondition(
                                null,
                                null,
                                "MAIL_RETENTION_POLICY_UPDATE",
                                null,
                                null,
                                null,
                                null,
                                null,
                                1,
                                20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).targetType()).isEqualTo("MAIL_RETENTION_POLICY");
        assertThat(result.content().get(0).actionArea()).isEqualTo("MAIL_RETENTION_POLICY");
        assertThat(result.content().get(0).actionName()).isEqualTo("UPDATE");
        assertThat(result.content().get(0).targetId()).isEqualTo(POLICY_ID);
    }

    private AdminAuditLog log(
            String targetType, UUID targetId, String actionArea, String actionName) {
        return new AdminAuditLog(
                UUID.randomUUID(),
                ACTOR_ID,
                "Admin",
                targetType,
                targetId,
                actionArea,
                actionName,
                AuditResult.SUCCESS,
                "{}",
                "{}",
                "127.0.0.1",
                "JUnit",
                OCCURRED_AT);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        AuthUserOrgJpaConfig.class,
        JpaAdminAuditLogRepositoryAdapter.class
    })
    static class TestApplication {}
}
