package com.meetbowl.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** validation 실패처럼 특정 입력 필드와 사유를 전달할 때 사용한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(String sheetName, Integer rowNumber, String field, String reason) {

    public ErrorDetail(String field, String reason) {
        this(null, null, field, reason);
    }
}
