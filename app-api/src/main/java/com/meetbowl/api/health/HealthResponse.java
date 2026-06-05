package com.meetbowl.api.health;

import java.time.Instant;

/** 헬스 체크 응답도 공통 성공 응답의 data 안에 담겨 내려간다. */
public record HealthResponse(String status, Instant checkedAt) {}
