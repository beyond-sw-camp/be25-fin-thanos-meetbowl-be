package com.meetbowl.application.user;

import java.util.UUID;

public record ChangeMyPasswordCommand(
        UUID userId, String currentPassword, String newPassword, String newPasswordConfirm) {}
