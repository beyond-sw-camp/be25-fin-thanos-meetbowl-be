package com.meetbowl.domain.meeting;

import java.time.Instant;

/** Infrastructure가 생성한 LiveKit 접속 토큰 결과다. */
public record LiveKitTokenIssueResult(
        String livekitUrl, String token, Instant issuedAt, Instant expiresAt) {}
