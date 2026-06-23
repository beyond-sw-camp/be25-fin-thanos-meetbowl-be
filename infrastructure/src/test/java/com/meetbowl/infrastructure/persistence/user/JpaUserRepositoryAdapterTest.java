package com.meetbowl.infrastructure.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;
import com.meetbowl.infrastructure.config.ElasticsearchUserSearchConfig;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.AuthUserOrgJpaConfig;
import com.meetbowl.infrastructure.persistence.organization.JpaAffiliateRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.organization.JpaDepartmentRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.organization.JpaPositionRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.organization.JpaTeamRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.organization.SpringDataAffiliateRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataDepartmentRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataPositionRepository;
import com.meetbowl.infrastructure.persistence.organization.SpringDataTeamRepository;
import com.meetbowl.infrastructure.search.user.ElasticsearchUserSearchAdapter;

@SpringBootTest(classes = JpaUserRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:user-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
            "meetbowl.elasticsearch.base-url=http://127.0.0.1:65535",
            "meetbowl.elasticsearch.user-index-name=meetbowl-users",
            "meetbowl.elasticsearch.auto-create-index=false",
            "meetbowl.elasticsearch.reindex-batch-size=50"
        })
class JpaUserRepositoryAdapterTest {

    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
    private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");

    @Autowired private JpaUserRepositoryAdapter adapter;
    @Autowired private SpringDataUserRepository springDataUserRepository;
    @Autowired private SpringDataAffiliateRepository affiliateRepository;
    @Autowired private SpringDataDepartmentRepository departmentRepository;
    @Autowired private SpringDataTeamRepository teamRepository;
    @Autowired private SpringDataPositionRepository positionRepository;
    @Autowired private JpaAffiliateRepositoryAdapter affiliateAdapter;
    @Autowired private JpaDepartmentRepositoryAdapter departmentAdapter;
    @Autowired private JpaTeamRepositoryAdapter teamAdapter;
    @Autowired private JpaPositionRepositoryAdapter positionAdapter;

    @BeforeEach
    void setUp() {
        springDataUserRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();
        positionRepository.deleteAll();
        affiliateRepository.deleteAll();

        affiliateAdapter.save(affiliate("Service Affiliate"));
        departmentAdapter.save(department("Service Development"));
        teamAdapter.save(team("Platform Team"));
        positionAdapter.save(position("Assistant Manager"));

        springDataUserRepository.save(
                UserEntity.from(
                        user(
                                UUID.fromString("00000000-0000-0000-0000-000000000201"),
                                "zzanggu",
                                "\uC9F1\uAD6C",
                                "zzanggu@example.com",
                                UserRole.USER,
                                AFFILIATE_ID,
                                DEPARTMENT_ID,
                                TEAM_ID,
                                POSITION_ID)));
        springDataUserRepository.save(
                UserEntity.from(
                        user(
                                UUID.fromString("00000000-0000-0000-0000-000000000202"),
                                "admin-user",
                                "Admin User",
                                "admin@local.meetbowl",
                                UserRole.ADMIN,
                                AFFILIATE_ID,
                                DEPARTMENT_ID,
                                TEAM_ID,
                                POSITION_ID)));
        springDataUserRepository.save(
                UserEntity.from(
                        user(
                                UUID.fromString("00000000-0000-0000-0000-000000000203"),
                                "null-org",
                                "No Org",
                                "nogroup@example.com",
                                UserRole.USER,
                                null,
                                null,
                                null,
                                null)));
    }

    @Test
    void findPageSupportsPartialSearchAcrossUserOrganizationAndRoleFields() {
        assertThat(loginIds(adapter.findPage("\uC9F1", 1, 20))).contains("zzanggu");
        assertThat(loginIds(adapter.findPage("ADMIN@LOCAL", 1, 20))).contains("admin-user");
        assertThat(loginIds(adapter.findPage("\uC11C\uBE44\uC2A4", 1, 20)))
                .contains("zzanggu", "admin-user");
        assertThat(loginIds(adapter.findPage("\uB300", 1, 20))).contains("zzanggu", "admin-user");
        assertThat(loginIds(adapter.findPage("ADMIN", 1, 20))).contains("admin-user");
        assertThat(loginIds(adapter.findPage("\uAD00\uB9AC\uC790", 1, 20))).contains("admin-user");
        assertThat(loginIds(adapter.findPage("\uC77C\uBC18 \uC0AC\uC6A9\uC790", 1, 20)))
                .contains("zzanggu", "null-org");
    }

    @Test
    void searchSupportsPartialSearchAcrossUserAndOrganizationFieldsIgnoringCase() {
        assertThat(
                        loginIds(
                                adapter.search(
                                        "ZZANG",
                                        null,
                                        null,
                                        null,
                                        null,
                                        UserStatus.ACTIVE,
                                        NOW,
                                        NOW.plusSeconds(24 * 60 * 60),
                                        1,
                                        20)))
                .contains("zzanggu");
        assertThat(
                        loginIds(
                                adapter.search(
                                        "platform",
                                        null,
                                        null,
                                        null,
                                        null,
                                        UserStatus.ACTIVE,
                                        NOW,
                                        NOW.plusSeconds(24 * 60 * 60),
                                        1,
                                        20)))
                .contains("admin-user", "zzanggu");
        assertThat(
                        loginIds(
                                adapter.search(
                                        "\uAD00\uB9AC\uC790",
                                        null,
                                        null,
                                        null,
                                        null,
                                        UserStatus.ACTIVE,
                                        NOW,
                                        NOW.plusSeconds(24 * 60 * 60),
                                        1,
                                        20)))
                .isEmpty();
    }

    private java.util.List<String> loginIds(Paged<User> page) {
        return page.content().stream().map(User::loginId).toList();
    }

    private Affiliate affiliate(String name) {
        return new Affiliate(AFFILIATE_ID, name, "AFF", ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private Department department(String name) {
        return new Department(
                DEPARTMENT_ID,
                AFFILIATE_ID,
                null,
                name,
                "DEP",
                ReferenceStatus.ACTIVE,
                1,
                NOW,
                NOW);
    }

    private Team team(String name) {
        return new Team(TEAM_ID, DEPARTMENT_ID, name, "TEAM", ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private Position position(String name) {
        return new Position(POSITION_ID, name, "POS", ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private User user(
            UUID userId,
            String loginId,
            String name,
            String email,
            UserRole role,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId) {
        return User.of(
                userId,
                loginId,
                "hash",
                name,
                email,
                role,
                UserStatus.ACTIVE,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                false,
                NOW,
                null,
                NOW,
                NOW);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        ElasticsearchUserSearchConfig.class,
        ElasticsearchUserSearchAdapter.class,
        AuthUserOrgJpaConfig.class,
        JpaUserRepositoryAdapter.class,
        JpaAffiliateRepositoryAdapter.class,
        JpaDepartmentRepositoryAdapter.class,
        JpaTeamRepositoryAdapter.class,
        JpaPositionRepositoryAdapter.class
    })
    static class TestApplication {}
}
