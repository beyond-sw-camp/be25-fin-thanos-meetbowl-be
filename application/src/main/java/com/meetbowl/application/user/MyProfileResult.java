package com.meetbowl.application.user;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResult(
        UUID userId,
        String loginId,
        String name,
        String email,
        String role,
        String status,
        UUID affiliateId,
        String affiliate,
        String department,
        String team,
        String position,
        Instant activeFrom,
        Instant activeUntil) {}
