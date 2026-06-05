package com.meetbowl.application.sampletask;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.sampletask.SampleTask;

/** 샘플 UseCase의 출력 모델이다. app-api는 이 Result를 API Response DTO로 변환한다. */
public record SampleTaskResult(
        UUID sampleTaskId, UUID ownerUserId, String title, Instant createdAt) {

    public static SampleTaskResult from(SampleTask sampleTask) {
        return new SampleTaskResult(
                sampleTask.id(),
                sampleTask.ownerUserId(),
                sampleTask.title(),
                sampleTask.createdAt());
    }
}
