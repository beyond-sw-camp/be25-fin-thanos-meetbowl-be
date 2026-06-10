package com.meetbowl.domain.common;

import java.util.List;

/**
 * 도메인 계층의 페이지 조회 결과를 담는 경량 타입이다. Spring Data 등 외부 페이지 타입을 도메인 port 계약에 노출하지 않기 위해 사용한다. 화면용 페이지 메타
 * 계산/응답 변환은 상위 계층이 담당한다.
 */
public record Paged<T>(List<T> content, long totalElements) {

    public Paged {
        content = List.copyOf(content);
    }
}
