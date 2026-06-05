package com.meetbowl.api.sampletask;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.sampletask.SampleTaskResult;

/** 샘플 API 응답 DTO다. application Result를 외부 응답 계약에 맞게 변환하는 위치를 보여준다. */
public record SampleTaskResponse(
        UUID sampleTaskId, UUID ownerUserId, String title, Instant createdAt) {

    public static SampleTaskResponse from(SampleTaskResult result) {
        return new SampleTaskResponse(
                result.sampleTaskId(), result.ownerUserId(), result.title(), result.createdAt());
    }
}
