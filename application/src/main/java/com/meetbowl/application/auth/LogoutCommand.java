package com.meetbowl.application.auth;

import java.util.UUID;

public record LogoutCommand(UUID userId) {}
