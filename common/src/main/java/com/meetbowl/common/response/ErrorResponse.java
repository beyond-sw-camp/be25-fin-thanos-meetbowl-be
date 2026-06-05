package com.meetbowl.common.response;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<ErrorDetail> details
) {
}
