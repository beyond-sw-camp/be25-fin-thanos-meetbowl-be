package com.meetbowl.api.health;

import java.time.Instant;

public record HealthResponse(String status, Instant checkedAt) {}
