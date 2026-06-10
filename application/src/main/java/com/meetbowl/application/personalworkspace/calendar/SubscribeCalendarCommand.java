package com.meetbowl.application.personalworkspace.calendar;

import java.util.UUID;

public record SubscribeCalendarCommand(UUID subscriberUserId, UUID targetUserId) {}
