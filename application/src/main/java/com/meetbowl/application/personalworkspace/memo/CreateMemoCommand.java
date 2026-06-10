package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

public record CreateMemoCommand(UUID ownerUserId, String title, String content) {}
