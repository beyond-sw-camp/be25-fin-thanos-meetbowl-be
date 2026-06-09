package com.meetbowl.infrastructure.persistence.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;

class TeamEntityTest {

    @Test
    void convert_success_between_domain_and_entity() {
        UUID teamId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Team team =
                new Team(
                        teamId,
                        departmentId,
                        "개발팀",
                        "DEV",
                        ReferenceStatus.ACTIVE,
                        1,
                        Instant.parse("2026-06-09T00:00:00Z"),
                        Instant.parse("2026-06-09T00:00:00Z"));

        Team converted = TeamEntity.from(team).toDomain();

        assertEquals(teamId, converted.id());
        assertEquals(departmentId, converted.departmentId());
        assertEquals("개발팀", converted.name());
        assertEquals(ReferenceStatus.ACTIVE, converted.status());
    }
}
