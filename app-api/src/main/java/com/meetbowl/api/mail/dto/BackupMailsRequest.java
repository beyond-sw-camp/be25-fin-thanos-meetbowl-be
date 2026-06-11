package com.meetbowl.api.mail.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BackupMailsRequest(
        @NotEmpty(message = "백업할 메일 ID는 필수입니다.")
                @Size(max = 100, message = "메일은 한 번에 최대 100개까지 백업할 수 있습니다.")
                List<@NotNull(message = "메일 ID에 빈 값을 포함할 수 없습니다.") UUID> mailIds) {}
