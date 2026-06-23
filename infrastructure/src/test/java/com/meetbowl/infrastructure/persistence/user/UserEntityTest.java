package com.meetbowl.infrastructure.persistence.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

class UserEntityTest {

    @Test
    void convert_success_between_domain_and_entity() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-08T08:00:00Z");
        User user =
                User.of(
                        userId,
                        "user01",
                        "passwordHash",
                        "Test User",
                        "user01@example.com",
                        UserRole.ADMIN,
                        UserStatus.ACTIVE,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        true,
                        now.minusSeconds(60),
                        now.plusSeconds(60),
                        now,
                        now);

        User converted = UserEntity.from(user).toDomain();

        assertEquals(userId, converted.id());
        assertEquals(UserRole.ADMIN, converted.role());
        assertEquals(UserStatus.ACTIVE, converted.status());
        assertEquals("passwordHash", converted.passwordHash());
        assertEquals(true, converted.initialPasswordChangeRequired());
    }
}
