package com.meetbowl.application.sampletask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.domain.sampletask.SampleTaskRepositoryPort;

class CreateSampleTaskUseCaseTest {

    @Test
    void createSampleTask() {
        FakeSampleTaskRepositoryPort sampleTaskRepositoryPort = new FakeSampleTaskRepositoryPort();
        Instant fixedNow = Instant.parse("2099-01-01T01:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        CreateSampleTaskUseCase useCase =
                new CreateSampleTaskUseCase(sampleTaskRepositoryPort, clock);
        UUID ownerUserId = UUID.randomUUID();

        SampleTaskResult result =
                useCase.execute(new CreateSampleTaskCommand(ownerUserId, "샘플 작업"));

        assertNotNull(result.sampleTaskId());
        assertEquals(ownerUserId, result.ownerUserId());
        assertEquals("샘플 작업", result.title());
        assertEquals(fixedNow, result.createdAt());
        assertEquals(result.sampleTaskId(), sampleTaskRepositoryPort.savedSampleTask.id());
    }

    private static class FakeSampleTaskRepositoryPort implements SampleTaskRepositoryPort {

        private SampleTask savedSampleTask;

        @Override
        public SampleTask save(SampleTask sampleTask) {
            this.savedSampleTask =
                    SampleTask.of(
                            UUID.randomUUID(),
                            sampleTask.ownerUserId(),
                            sampleTask.title(),
                            sampleTask.createdAt());
            return savedSampleTask;
        }
    }
}
