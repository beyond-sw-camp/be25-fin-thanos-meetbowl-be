package com.meetbowl.application.sampletask;

import java.util.UUID;

/** 샘플 생성 UseCase의 입력 모델이다. API Request DTO를 application 계층으로 직접 전달하지 않는 예시다. */
public record CreateSampleTaskCommand(UUID ownerUserId, String title) {}
