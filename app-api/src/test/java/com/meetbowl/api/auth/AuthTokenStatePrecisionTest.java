package com.meetbowl.api.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.flyway.enabled=false",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "spring.rabbitmq.listener.direct.auto-startup=false",
            "spring.rabbitmq.dynamic=false"
        })
class AuthTokenStatePrecisionTest {

    @Autowired private ApplicationContext applicationContext;

    @Test
    void tokenIssuedWithinSameSecondAsSessionRevocationRemainsValid() {
        UUID userId = UUID.randomUUID();
        Instant revokedAt = Instant.parse("2026-06-19T01:07:28.900Z");
        Object tokenStateRepositoryPort =
                applicationContext.getBean("redisTokenStateRepositoryAdapter");

        invoke(tokenStateRepositoryPort, "revokeUserSessions", userId, revokedAt);

        assertFalse(
                (boolean)
                        invoke(
                                tokenStateRepositoryPort,
                                "isUserSessionRevoked",
                                userId,
                                Instant.parse("2026-06-19T01:07:28Z")));
        assertTrue(
                (boolean)
                        invoke(
                                tokenStateRepositoryPort,
                                "isUserSessionRevoked",
                                userId,
                                Instant.parse("2026-06-19T01:07:27Z")));
    }

    private Object invoke(Object target, String methodName, Object... arguments) {
        try {
            Class<?>[] parameterTypes =
                    java.util.Arrays.stream(arguments).map(Object::getClass).toArray(Class<?>[]::new);
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
