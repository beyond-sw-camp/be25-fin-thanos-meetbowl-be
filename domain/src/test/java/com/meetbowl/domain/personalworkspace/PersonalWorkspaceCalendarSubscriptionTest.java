package com.meetbowl.domain.personalworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class PersonalWorkspaceCalendarSubscriptionTest {

    @Test
    void cannotSubscribeOwnCalendar() {
        UUID userId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                PersonalWorkspaceCalendarSubscription.create(
                                        userId, userId, Instant.parse("2099-01-01T01:00:00Z")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
