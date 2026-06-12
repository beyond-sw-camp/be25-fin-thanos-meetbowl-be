package com.meetbowl.api.mail.dto;

import jakarta.validation.constraints.NotNull;

/** 메일 읽음/안읽음 변경 요청 본문이다. true면 읽음, false면 안읽음으로 처리한다. */
public record ChangeMailReadRequest(@NotNull(message = "읽음 상태는 필수입니다.") Boolean read) {}
