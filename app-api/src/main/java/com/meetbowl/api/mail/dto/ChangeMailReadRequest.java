package com.meetbowl.api.mail.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeMailReadRequest(@NotNull(message = "읽음 상태는 필수입니다.") Boolean read) {}
