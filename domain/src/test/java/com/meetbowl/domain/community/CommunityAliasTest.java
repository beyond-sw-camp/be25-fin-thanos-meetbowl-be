package com.meetbowl.domain.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class CommunityAliasTest {

    @Test
    void displayNameIsAnonymousPlusNumber() {
        CommunityAlias alias = CommunityAlias.create(UUID.randomUUID(), 1);

        assertEquals("익명1", alias.displayName());
    }

    @Test
    void rejectsNonPositiveAliasNo() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> CommunityAlias.create(UUID.randomUUID(), 0));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}