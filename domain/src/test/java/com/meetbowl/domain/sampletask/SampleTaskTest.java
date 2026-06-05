package com.meetbowl.domain.sampletask;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SampleTaskTest {

    @Test
    void createSampleTask() {
        UUID ownerUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2099-01-01T01:00:00Z");

        SampleTask sampleTask = SampleTask.create(ownerUserId, "샘플 작업", createdAt);

        assertEquals(null, sampleTask.id());
        assertEquals(ownerUserId, sampleTask.ownerUserId());
        assertEquals("샘플 작업", sampleTask.title());
        assertEquals(createdAt, sampleTask.createdAt());
    }

    @Test
    void restoreSampleTaskWithId() {
        UUID id = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2099-01-01T01:00:00Z");

        SampleTask sampleTask = SampleTask.of(id, ownerUserId, "샘플 작업", createdAt);

        assertEquals(id, sampleTask.id());
        assertEquals(ownerUserId, sampleTask.ownerUserId());
        assertEquals("샘플 작업", sampleTask.title());
        assertEquals(createdAt, sampleTask.createdAt());
    }

    @Test
    void titleMustNotBeBlank() {
        BusinessException exception = assertThrows(BusinessException.class, () -> SampleTask.create(
                UUID.randomUUID(),
                " ",
                Instant.parse("2099-01-01T01:00:00Z")
        ));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
