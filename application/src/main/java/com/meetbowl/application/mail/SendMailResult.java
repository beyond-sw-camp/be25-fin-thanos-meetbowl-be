package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.UUID;

public record SendMailResult(UUID mailId, String deliveryStatus, Instant requestedAt) {}
