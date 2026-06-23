package com.meetbowl.application.user;

import java.util.UUID;

public record UpdateMyProfileCommand(UUID userId, String name, String email) {}
