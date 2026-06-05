package com.meetbowl.application.sampletask;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.domain.sampletask.SampleTaskRepositoryPort;

/** 샘플 작업 생성 흐름을 보여주는 UseCase다. sample 또는 sample-jpa 프로필에서만 Bean으로 등록되어 기본 실행에는 포함되지 않는다. */
@Profile("sample | sample-jpa")
@Service
public class CreateSampleTaskUseCase {

    private final SampleTaskRepositoryPort sampleTaskRepositoryPort;
    private final Clock clock;

    public CreateSampleTaskUseCase(SampleTaskRepositoryPort sampleTaskRepositoryPort) {
        this(sampleTaskRepositoryPort, Clock.systemUTC());
    }

    CreateSampleTaskUseCase(SampleTaskRepositoryPort sampleTaskRepositoryPort, Clock clock) {
        this.sampleTaskRepositoryPort = sampleTaskRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public SampleTaskResult execute(CreateSampleTaskCommand command) {
        SampleTask sampleTask =
                SampleTask.create(command.ownerUserId(), command.title(), Instant.now(clock));
        SampleTask savedSampleTask = sampleTaskRepositoryPort.save(sampleTask);
        return SampleTaskResult.from(savedSampleTask);
    }
}
